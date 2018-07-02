import classes.*;
import com.sun.jnlp.ApiDialog;
import forms.MainGUIForm;
import forms.dialogs.AreYouSureDialogProcessor;
import iniSettings.INISettings;
import iniSettings.INISettingsRecord;
import iniSettings.INISettingsSection;
import iniSettings.exceptions.AlreadyExistsException;
import iniSettings.exceptions.IniSettingsException;
import iniSettings.exceptions.NotFoundException;
import jssc.SerialPortList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.rmi.UnexpectedException;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeoutException;


@SuppressWarnings("FieldCanBeLocal")
class BORT_debug_manager implements ActionListener, DebugInformationListener {

    //Максимальное время ожидания ответа от модуля при подключении к порту
    private final long MAX_CONNECTION_WAIT_TIMEOUT = 3000;
    //Максимальное время ожидания ответа от модуля при отправке какого - либо запроса
    private final long RESPONSE_WAIT_TIMEOUT = 300;
    private final Primitive_KeyValueRecord MODULE_PARAMETERS_LIST[] = {
            new Primitive_KeyValueRecord(1, "Интервал вывода информации (миллисекунд)"),
            new Primitive_KeyValueRecord(2, "Интервал подсчёта оборотов в минуту (миллисекунд)"),
            new Primitive_KeyValueRecord(3, "Интервал измерения напряжений (миллисекунд)"),
            new Primitive_KeyValueRecord(4, "Интервал обработки датчиков (миллисекунд)"),
            new Primitive_KeyValueRecord(5, "Интервал основного действия (миллисекунд)"),
            new Primitive_KeyValueRecord(6, "Интервал сигнала о ручнике (миллисекунд)"),
            new Primitive_KeyValueRecord(7, "Интервал обработки входящих команд (миллисекунд)"),
            new Primitive_KeyValueRecord(37, "Интервал оповещения о низком уровне топлива (миллисекунд)"),
            new Primitive_KeyValueRecord(33, "Время для пробуждения контроллера (миллисекунд)"),
            new Primitive_KeyValueRecord(10, "Интервал смены надписи статуса (миллисекунд)"),
            new Primitive_KeyValueRecord(8, "Интервал сохранения статистики (секунд)"),
            new Primitive_KeyValueRecord(13, "Включить возможность глубокого сна (0/1)"),
            new Primitive_KeyValueRecord(22, "Уровень резервного напряжения"),
            new Primitive_KeyValueRecord(23, "Минимальное пороговое няпряжение для оповещения"),
            new Primitive_KeyValueRecord(24, "Максимальное пороговое няпряжение для оповещения"),
            new Primitive_KeyValueRecord(25, "Уровень приглушённой подсветки (0-255)"),
            new Primitive_KeyValueRecord(26, "Уровень максимальной подсветки (0-255)"),
            new Primitive_KeyValueRecord(27, "Пороговые обороты, при которых оповещается о старте двигателя"),
            new Primitive_KeyValueRecord(28, "Пороговые обороты, при которых оповещается об остановке двигателя"),
            new Primitive_KeyValueRecord(29, "Пороговые обороты, при которых оповещается о поднятом ручнике"),
            new Primitive_KeyValueRecord(40, "Пороговый уровень температуры для сигнализации о перегреве"),
            new Primitive_KeyValueRecord(36, "Минимальный уровень топлива для оповещения (литров)"),
            new Primitive_KeyValueRecord(34, "Моточасы"),
            new Primitive_KeyValueRecord(35, "Количество оборотов моточасов (0-100000)"),
    };
    private final int RECONNECT_ERRORS_THRESHOLD = 3;
    private BORT_connection connection;
    private MainGUIForm mainGUIForm;
    private volatile ActionEvent actionEvent = null;
    private int connectionErrorsCounter = 0;

    public static void main(String[] args) {
        System.out.println("BORT DEBUG MANAGER");
        new BORT_debug_manager().run();
    }

    private void run() {
        mainGUIForm = new MainGUIForm(new Dimension(800, 500), this);
        connection = findAndConnectToTheModule();
        checkRTCparams();
        while (mainGUIForm.isShowing()) {
            if (connectionErrorsCounter >= RECONNECT_ERRORS_THRESHOLD || connection == null || !connection.getSerialPort().isOpened()) {
                mainGUIForm.updateStatus("Соединение потеряно. Переподключаемся...");
                if (connection != null) {
                    connection.close();
                }
                connection = findAndConnectToTheModule();
                connectionErrorsCounter = 0;
                checkRTCparams();
            }
            delayMs(220);
            try {
                requestStatistics();
                if (actionEvent != null) {
                    interpretateAction();
                }
                connectionErrorsCounter = 0;
            } catch (InterruptedException e) {
                mainGUIForm.updateStatus("Операция прервана");
            } catch (TimeoutException e) {
                connectionErrorsCounter++;
                mainGUIForm.updateErrorStatus("Ошибка получения данных: таймаут");
            } catch (InvalidParameterException e) {
                mainGUIForm.updateErrorStatus("Неизвестная команда");
            }
            mainGUIForm.getProgressBar().setVisible(false);
            actionEvent = null;
        }
    }

    private void interpretateAction() throws TimeoutException, InterruptedException {
        switch (actionEvent.getActionCommand().toLowerCase().trim()) {
            case "set":
                setParameterDialog();
                break;
            case "restart":
                restartModule();
                break;
            case "export settings":
                exportModuleSettingsToFile();
                break;
            case "import settings":
                importParamsIntoModule();
                break;
            case "reset settings":
                reset_EEPROM();
                break;
            case "apply settings":
                saveSettingsToEEPROM();
                break;
            case "update paramslist":
            case "refresh settings":
                updateParamsList();
                break;
            case "hold connection":
                disconnectAndWaitForClickOnHoldConnectionCheckbox();
                break;
            case "reconnect":
                connection.close();
                connection = findAndConnectToTheModule();
                break;
            default:
                throw new InvalidParameterException();
        }
    }

    private void updateParamsList() throws InterruptedException {
        mainGUIForm.getParametersListPane().setEnabled(false);
        mainGUIForm.getSelectedParameterComboBox().setEnabled(false);
        mainGUIForm.setParamsList(readAllParametersFromModule(false));
    }

    private void checkRTCparams() {
        mainGUIForm.updateStatus("Проверка корректности установки времени...");
        try {
            isTimeOnRTCsetted();
            setTimeOnRTC();
        } catch (InterruptedException ignored) {
        } catch (TimeoutException e) {
            mainGUIForm.updateStatus("Ошибка получения данных: таймаут");
            connectionErrorsCounter++;
        }
        mainGUIForm.updateStatus("Время синхронизировано");
    }

    private void disconnectAndWaitForClickOnHoldConnectionCheckbox() {
        connection.close();
        mainGUIForm.updateStatus("Подключение разорвано");
        while (!mainGUIForm.getHoldConnectionCheckBox().isSelected()) {
            delayMs(100);
        }
        connection = null;
    }

    private void exportModuleSettingsToFile() throws InterruptedException {
        try {
            String gettedFileName;
            INISettings readedParameters = new INISettings();
            readedParameters.addSection(readAllParametersFromModule(true));
            JFileChooser fileopen = new JFileChooser();
            fileopen.setCurrentDirectory(new File("."));
            int ret = fileopen.showDialog(null, "Выбрать файл для сохранения настроек");
            if (ret == JFileChooser.APPROVE_OPTION) {
                gettedFileName = fileopen.getSelectedFile().getAbsolutePath();
            } else {
                throw new InterruptedException();
            }

            mainGUIForm.updateStatus("Сохраняем настройки в файл...");
            readedParameters.saveToFile(gettedFileName);
            mainGUIForm.updateStatus("Настройки успешно сохранены!");
        } catch (UnexpectedException e) {
            mainGUIForm.updateErrorStatus(e.getMessage());
        } catch (IOException e) {
            mainGUIForm.updateErrorStatus("Ошибка ввода-вывода во время записи в файл: " + e.getMessage());
        } catch (AlreadyExistsException ignored) {
        }
    }

    private boolean isTimeOnRTCsetted() throws TimeoutException {
        mainGUIForm.updateStatus("Запрашиваем текущее время...");
        try {
            connection.send("$8:");
            BORT_responseType waitingResponseTypes[] = {BORT_responseType.ERR_RTC_CONNECTION_FAILED, BORT_responseType.ERR_RTC_UNSETTED, BORT_responseType.RTC_TIME_READ_SUCCESS};
            BORT_response gettedResponse = waitForIncomingResponse(waitingResponseTypes, RESPONSE_WAIT_TIMEOUT);
            if (gettedResponse.getResponseType() == BORT_responseType.RTC_TIME_READ_SUCCESS || gettedResponse.getResponseParams().getRecords().size() > 0) {
                mainGUIForm.updateStatus("Текущее время по RTC: " + gettedResponse.getResponseParams().getFieldByKey("RTC").getValue());
                return true;
            } else {
                mainGUIForm.updateErrorStatus(gettedResponse.toString());
            }
        } catch (NotFoundException e) {
            mainGUIForm.updateErrorStatus("Ошибка: некорректный ответ от модуля");
        }
        return false;
    }

    private void setTimeOnRTC() throws InterruptedException, TimeoutException {
        String currentTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
        mainGUIForm.updateStatus("Устанавливаем текущее время в RTC...");
        connection.send("$2:32=" + currentTime);
        BORT_responseType waitingResponseTypes[] = {BORT_responseType.ERR_WRONG_PARAM_NAME, BORT_responseType.PARAM_RW_SUCCESS};
        BORT_response gettedResponse = waitForIncomingResponse(waitingResponseTypes, RESPONSE_WAIT_TIMEOUT);
        if (gettedResponse.getResponseType() != BORT_responseType.ERR_WRONG_PARAM_NAME && gettedResponse.getResponseParams().getRecords().size() > 0) {
            mainGUIForm.updateStatus("Время успешно установлено, проверка...");
            if (!isTimeOnRTCsetted()) {
                mainGUIForm.updateErrorStatus("Ошибка при установке времени.");
                throw new InterruptedException();
            }
        } else {
            mainGUIForm.updateErrorStatus(gettedResponse.toString());
        }
    }

    private void restartModule() throws TimeoutException {
        connection.send("$1:");
        mainGUIForm.updateStatus("Перезагружаем модуль...");
        BORT_responseType waitingResponseTypes[] = {BORT_responseType.RESTART_CMD_GETTED};
        waitForIncomingResponse(waitingResponseTypes, RESPONSE_WAIT_TIMEOUT);
        mainGUIForm.updateStatus("Ответ получен. Модуль перезагружается...");
        waitingResponseTypes = new BORT_responseType[1];
        waitingResponseTypes[0] = BORT_responseType.MODULE_STARTED;
        waitForIncomingResponse(waitingResponseTypes, RESPONSE_WAIT_TIMEOUT + 2000);
        mainGUIForm.updateStatus("Модуль успешно перезагружен.");
    }

    private void setParameterDialog() throws InterruptedException, TimeoutException {
        try {
            int paramIndex = getParamIndexFromParamName((String) mainGUIForm.getSelectedParameterComboBox().getSelectedItem());
            if (mainGUIForm.getSelectedParamNewValueField().getText().equals("")) {
                mainGUIForm.updateErrorStatus("Новое значение параметра не может быть пустым");
            } else {
                writeParameter(paramIndex, mainGUIForm.getSelectedParamNewValueField().getText().trim());
                updateParamsList();
            }
        } catch (NotFoundException e) {
            mainGUIForm.updateErrorStatus("Параметра с данным названием не существует.");
        }
    }

    private void writeParameter(int paramId, String value) throws TimeoutException {
        connection.send("$2:" + paramId + '=' + value);
        mainGUIForm.updateStatus("Записываем параметр...");
        BORT_responseType waitingResponseTypes[] = {BORT_responseType.ERR_WRONG_PARAM_NAME, BORT_responseType.PARAM_RW_SUCCESS};
        BORT_response gettedResponse = waitForIncomingResponse(waitingResponseTypes, RESPONSE_WAIT_TIMEOUT);
        if (gettedResponse.getResponseType() != BORT_responseType.ERR_WRONG_PARAM_NAME && gettedResponse.getResponseParams().getRecords().size() > 0) {
            mainGUIForm.updateStatus("Параметр успешно установлен в значение \"" + gettedResponse.getResponseParams().getRecords().get(0).getValue() + '"');
        } else {
            mainGUIForm.updateErrorStatus(gettedResponse.toString());
        }
    }

    private int getParamIndexFromParamName(String paramName) throws NotFoundException {
        paramName = paramName.trim().toLowerCase();
        for (Primitive_KeyValueRecord currentParam : MODULE_PARAMETERS_LIST) {
            if (currentParam.value.trim().toLowerCase().equals(paramName.trim().toLowerCase())) {
                return currentParam.key;
            }
        }
        throw new NotFoundException();
    }

    private void importParamsIntoModule() throws InterruptedException, TimeoutException {
        String gettedFileName;
        JFileChooser fileopen = new JFileChooser();
        fileopen.setCurrentDirectory(new File("."));
        int ret = fileopen.showDialog(null, "Выбрать файл для загрузки настроек");
        if (ret == JFileChooser.APPROVE_OPTION) {
            gettedFileName = fileopen.getSelectedFile().getAbsolutePath();
        } else {
            throw new InterruptedException();
        }

        if (new AreYouSureDialogProcessor().showDialog("ВЫ УВЕРЕНЫ В ТОМ, ЧТО ХОТИТЕ ЗАМЕНИТЬ ВСЕ СОХРАНЁННЫЕ ДАННЫЕ И НАСТРОЙКИ МОДУЛЯ НА НОВЫЕ?\n" +
                "ЭТО ДЕЙСТВИЕ НЕВОЗМОЖНО ОТМЕНИТЬ!!!") != ApiDialog.DialogResult.OK) {
            throw new InterruptedException();
        }
        INISettings loadedSettings = new INISettings();
        try {
            loadedSettings.loadFromFile(gettedFileName);
            writeAllParametersIntoModule(loadedSettings.getSectionByName("BORT_PARAMS"));
            saveSettingsToEEPROM();
            restartModule();
            updateParamsList();
            mainGUIForm.updateStatus("Операция выполнена");
        } catch (IOException e) {
            mainGUIForm.updateErrorStatus("Ошибка ввода - вывода при чтении из файла: " + e.getMessage());
        } catch (IniSettingsException e) {
            mainGUIForm.updateErrorStatus("Ошибка при обработке файла: " + e.getMessage());
        } catch (IndexOutOfBoundsException e) {
            mainGUIForm.updateErrorStatus("Ошибка при обработке файла - ошибка поиска ключа: " + e.getMessage());
        }
    }

    /**
     * Записывает все параметры в модуль из секции настроек parameters
     *
     * @param parameters Настройки, которые нужно записать в модуль
     */
    private void writeAllParametersIntoModule(INISettingsSection parameters) {
        mainGUIForm.updateStatus("Записываем настройки в модуль...");
        int paramsWritedCounter = 0;
        boolean allWritedSucessfully = true;
        mainGUIForm.getProgressBar().setVisible(true);
        mainGUIForm.getProgressBar().setMaximum(parameters.getRecords().size());
        mainGUIForm.getProgressBar().setMinimum(0);
        for (INISettingsRecord currentParameter : parameters.getRecords()) {
            try {
                mainGUIForm.getProgressBar().setValue(++paramsWritedCounter);
                mainGUIForm.updateStatus("Запись " + paramsWritedCounter + " из " + parameters.getRecords().size() + " - " + currentParameter.getKey() + "...");
                connection.send("$2:" + getParamIndexFromParamName(currentParameter.getKey()) + '=' + currentParameter.getValue());
                BORT_responseType waitingResponseTypes[] = {BORT_responseType.ERR_WRONG_PARAM_NAME, BORT_responseType.PARAM_RW_SUCCESS};
                BORT_response gettedResponse = waitForIncomingResponse(waitingResponseTypes, RESPONSE_WAIT_TIMEOUT);
                if (gettedResponse.getResponseType() == BORT_responseType.ERR_WRONG_PARAM_NAME || gettedResponse.getResponseParams().getRecords().size() == 0) {
                    mainGUIForm.updateStatus("\rОшибка: " + gettedResponse);
                }
            } catch (NotFoundException e) {
                mainGUIForm.updateErrorStatus("Ошибка: параметра с названием \"" + currentParameter.getKey() + "\" не существует.");
                allWritedSucessfully = false;
            } catch (TimeoutException e) {
                mainGUIForm.updateErrorStatus("Ошибка связи: таймаут при записи параметра \"" + currentParameter.getKey() + "\"");
                allWritedSucessfully = false;
            }
        }
        mainGUIForm.updateStatus("Запись параметров завершена " + (allWritedSucessfully ? "успешно" : "с ошибками"));
    }

    /**
     * Запрашивает все параметры из модуля и возвращает их в виде экземпляра INISettingsSection.
     * В случае, если чтение параметров из модуля не было успешным, и параметр notifyIfNotSucessful был true,
     * спрашивает пользователя, хочет ли он продолжить.
     *
     * @param notifyIfNotSucessful Спрашивать ли пользователя о продолжении операции, если она не была успешна.
     * @return Список параметров в виде INISettingsSection
     * @throws InterruptedException В случае, если пользователь отказался продолжать выполнение операции.
     */
    private INISettingsSection readAllParametersFromModule(boolean notifyIfNotSucessful) throws InterruptedException {
        INISettingsSection readedParamsList = new INISettingsSection("BORT_PARAMS");
        mainGUIForm.updateStatus("Запрашиваем параметры...");
        boolean allIsSuccessful = true;
        try {
            int counter = 0;
            mainGUIForm.getProgressBar().setVisible(true);
            mainGUIForm.getProgressBar().setMaximum(MODULE_PARAMETERS_LIST.length);
            mainGUIForm.getProgressBar().setMinimum(0);
            for (Primitive_KeyValueRecord currentParam : MODULE_PARAMETERS_LIST) {
                connection.send("$3:" + currentParam.key + '=');
                mainGUIForm.getProgressBar().setValue(++counter);
                mainGUIForm.updateStatus("Запрос параметра " + counter + " из " + MODULE_PARAMETERS_LIST.length + " - " + currentParam.value + "...");
                try {
                    BORT_responseType waitingResponseTypes[] = {BORT_responseType.ERR_WRONG_PARAM_NAME, BORT_responseType.PARAM_RW_SUCCESS};
                    BORT_response gettedResponse = waitForIncomingResponse(waitingResponseTypes, RESPONSE_WAIT_TIMEOUT);
                    if (gettedResponse.getResponseType() != BORT_responseType.ERR_WRONG_PARAM_NAME && gettedResponse.getResponseParams().getRecords().size() > 0) {
                        readedParamsList.addField(new INISettingsRecord(currentParam.value, gettedResponse.getResponseParams().getRecords().get(0).getValue()));
                    } else {
                        allIsSuccessful = false;
                    }
                } catch (TimeoutException e) {
                    allIsSuccessful = false;
                    mainGUIForm.updateStatus(currentParam.value + " - ошибка. Таймаут.");
                }
            }
        } catch (AlreadyExistsException ignored) {
        }
        if (!allIsSuccessful && notifyIfNotSucessful) {
            if (new AreYouSureDialogProcessor().showDialog("Возникли проблемы при чтении некоторых параметров. Вы хотите продолжить?") != ApiDialog.DialogResult.OK) {
                throw new InterruptedException("Ошибка: не удалось получить все параметры. Дальнейшие действия отменены.");
            }
        }
        mainGUIForm.updateStatus("Все параметры были получены");
        return readedParamsList;
    }

    /**
     * Запрашивает у пользователя разрешение на сброс энергонезависимой памяти модуля, и в случае успеха, отправляет запрос на сброс на модуль.
     * Ждёт ответа от модуля в течение RESPONSE_WAIT_TIMEOUT миллисекунд.
     *
     * @throws InterruptedException В случае, если пользователь отказался сбрасывать память контроллера.
     * @throws TimeoutException     В случае, если ответ от модуля не был получен в течение RESPONSE_WAIT_TIMEOUT миллисекунд.
     */
    private void reset_EEPROM() throws InterruptedException, TimeoutException {
        if (new AreYouSureDialogProcessor().showDialog("ВЫ УВЕРЕНЫ В ТОМ, ЧТО ХОТИТЕ СБРОСИТЬ ВСЕ СОХРАНЁННЫЕ ДАННЫЕ И НАСТРОЙКИ МОДУЛЯ?\n" +
                "ЭТО ДЕЙСТВИЕ НЕВОЗМОЖНО ОТМЕНИТЬ!!!") != ApiDialog.DialogResult.OK) {
            throw new InterruptedException();
        }
        mainGUIForm.updateStatus("Отправляем запрос на сброс...");
        connection.send("$2:38=1");
        BORT_responseType waitingResponseTypes[] = {BORT_responseType.EEPROM_RESET_SUCCESS};
        BORT_response gettedResponse = waitForIncomingResponse(waitingResponseTypes, RESPONSE_WAIT_TIMEOUT);
        mainGUIForm.updateStatus(gettedResponse.toString());
        restartModule();
        updateParamsList();
    }

    /**
     * Посылает на модуль запрос о сохранении параметров в энергонезависимую память, и ждёт ответа в течение RESPONSE_WAIT_TIMEOUT.
     *
     * @throws TimeoutException В случае, если ответ не был получен в течение RESPONSE_WAIT_TIMEOUT.
     */
    private void saveSettingsToEEPROM() throws TimeoutException {
        mainGUIForm.updateStatus("Отправляем запрос на сохранение...");
        connection.send("$4:");
        BORT_responseType waitingResponseTypes[] = {BORT_responseType.EEPROM_SAVE_CMD_GETTED};
        BORT_response gettedResponse = waitForIncomingResponse(waitingResponseTypes, RESPONSE_WAIT_TIMEOUT);
        mainGUIForm.updateStatus(gettedResponse.toString());
    }

    /**
     * Посылает на модуль запрос о получении статистики, и в течение RESPONSE_WAIT_TIMEOUT миллисекунд ждёт от него ответа.
     * При получении ответа, обновляет информацию в пользовательском интерфейсе
     *
     * @throws TimeoutException В случае, если модуль не ответил вовремя.
     */
    private void requestStatistics() throws TimeoutException {
        connection.send("$7:");
        //mainGUIForm.updateStatus("Запрашиваем статистику...");
        BORT_responseType waitingResponseTypes[] = {BORT_responseType.INFO_STATISTICS};
        BORT_response gettedResponse = waitForIncomingResponse(waitingResponseTypes, RESPONSE_WAIT_TIMEOUT);
        mainGUIForm.updateStatistics(gettedResponse.toString());
    }

    /**
     * Метод ожидает получение определённого ответа от модуля в течение некоторого времени
     *
     * @param responseTypes Типы ответа от модуля, которые должны быть приняты. Все остальные типы будут проигнорированы
     * @param timeout       Максимальное время ожидания ответа. По истечении этого времени выбрасывается исключение TimeoutException
     * @return Ответ от модуля, который принадлежит одному из типов в массиве responseTypes
     * @throws TimeoutException В случае, если ответ от модуля не пришёл в течение timeout миллисекунд
     */
    private BORT_response waitForIncomingResponse(BORT_responseType responseTypes[], long timeout) throws TimeoutException {
        connection.clearResponsesStack();
        long capturedSysTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - capturedSysTime < timeout) {
            for (BORT_response currentResponse : connection.getResponsesStack()) {
                for (BORT_responseType currentResponseType : responseTypes) {
                    if (currentResponse.getResponseType() == currentResponseType) {
                        return currentResponse;
                    }
                }
            }
            delayMs(100);
        }
        throw new TimeoutException();
    }

    /**
     * Метод сканирует все com - порты системы на предмет наличия в них модуля инфракрасного приёмника, а найдя
     * автоматически подключается к нему и возвращает экземпляр класса @code{BORT_connection}.
     *
     * @return Экземпляр класса @code{BORT_connection}, являющийся обработчиком данных именно с этого модуля.
     */
    private BORT_connection findAndConnectToTheModule() {
        BORT_connection BORT_connection = null;
        mainGUIForm.updateStatus("Поиск и подключение...");
        while (BORT_connection == null) {
            if (SerialPortList.getPortNames().length > 0) {
                for (String selectedPort : SerialPortList.getPortNames()) {
                    long connection_startedTime = System.currentTimeMillis();
                    BORT_connection = new BORT_connection(selectedPort, this);
                    while (System.currentTimeMillis() - connection_startedTime < MAX_CONNECTION_WAIT_TIMEOUT
                            && !BORT_connection.isConnected()) {
                        delayMs(100); //Ждём до тех пор, пока не будет осуществлено подключение, либо пока не выйдет время.
                    }
                    if (BORT_connection.isConnected()) {
                        mainGUIForm.updateStatus("Подключение успешно!\nПорт: " + BORT_connection.getSerialPort().getPortName());
                        break;
                    } else {
                        BORT_connection.close();
                        BORT_connection = null;
                    }
                }
            }
        }
        return BORT_connection;
    }

    /**
     * Метод приостанавливает поток на @code{millis} миллисекунд.
     *
     * @param millis длительность паузы
     */
    private void delayMs(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Вызывается, когда пользователь производит определённые действия в интерфейсе.
     *
     * @param actionEvent Событие, вызванное пользовательским интерфейсом
     */
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        this.actionEvent = actionEvent;
    }

    @Override
    public void debugInformationGetted(BORT_response response) {
        String currentTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
        mainGUIForm.updateDebugStatusBar(currentTime + " | " + response.toString() + '\n');
    }
}
