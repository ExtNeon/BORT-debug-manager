import classes.*;
import com.sun.jnlp.ApiDialog;
import forms.MainGUIForm;
import forms.diagram.GraphDiagram;
import forms.diagram.GraphLine;
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
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.concurrent.TimeoutException;


@SuppressWarnings("FieldCanBeLocal")
class BORT_debug_manager implements ActionListener, DebugInformationListener {

    // TODO: 29.08.2018 Вывести все строковые литералы в константы и в отдельный языковой класс. Чисто для порядка.
    private static final String CONST_STR_DATA_RECEIVING_TIMEOUT = "Ошибка получения данных: таймаут";
    //Список параметров модуля
    private final Primitive_KeyValueRecord MODULE_PARAMETERS_LIST[] = {
            new Primitive_KeyValueRecord(1, "Интервал вывода информации (миллисекунд)"),
            new Primitive_KeyValueRecord(2, "Интервал подсчёта скорости вращения коленвала (миллисекунд)"),
            new Primitive_KeyValueRecord(3, "Интервал измерения напряжений (миллисекунд)"),
            new Primitive_KeyValueRecord(4, "Интервал обработки датчиков (миллисекунд)"),
            new Primitive_KeyValueRecord(5, "Интервал основного действия (миллисекунд)"),
            new Primitive_KeyValueRecord(6, "Интервал сигнала о ручнике (миллисекунд)"),
            new Primitive_KeyValueRecord(7, "Интервал обработки входящих команд (миллисекунд)"),
            new Primitive_KeyValueRecord(37, "Интервал оповещения о низком уровне топлива (миллисекунд)"),
            new Primitive_KeyValueRecord(33, "Время для пробуждения контроллера (миллисекунд)"),
            new Primitive_KeyValueRecord(10, "Интервал смены надписи статуса (миллисекунд)"),
            new Primitive_KeyValueRecord(8, "Интервал сохранения статистики (секунд)"),
            new Primitive_KeyValueRecord(48, "Период накопления данных об уровне топлива (миллисекунд)"),
            new Primitive_KeyValueRecord(13, "Включить возможность глубокого сна (0/1)"),
            new Primitive_KeyValueRecord(47, "Минимальное время простоя для инициации глубокого сна (миллисекунд)"),
            new Primitive_KeyValueRecord(22, "Порог напряжения для инициации глубокого сна"),
            new Primitive_KeyValueRecord(45, "Включить сглаживание вычислений скорости вращения коленчатого вала (0/1)"),
            new Primitive_KeyValueRecord(23, "Минимальное пороговое няпряжение для оповещения"),
            new Primitive_KeyValueRecord(24, "Максимальное количество ошибок дисплея для его сброса (если 0, то выкл)"),
            new Primitive_KeyValueRecord(25, "Уровень приглушённой подсветки (0-255)"),
            new Primitive_KeyValueRecord(26, "Уровень максимальной подсветки (0-255)"),
            new Primitive_KeyValueRecord(27, "Пороговые обороты, при которых оповещается о старте двигателя"),
            new Primitive_KeyValueRecord(28, "Пороговые обороты, при которых оповещается об остановке двигателя"),
            new Primitive_KeyValueRecord(29, "Пороговые обороты, при которых оповещается о поднятом ручнике"),
            new Primitive_KeyValueRecord(46, "Пороговые обороты, при которых топливный насос будет работать постоянно"),
            new Primitive_KeyValueRecord(40, "Пороговый уровень температуры для сигнализации о перегреве"),
            new Primitive_KeyValueRecord(36, "Минимальный уровень топлива для оповещения (литров)"),
            new Primitive_KeyValueRecord(34, "Моточасы"),
            new Primitive_KeyValueRecord(35, "Количество оборотов моточасов (0-100000)"),
            new Primitive_KeyValueRecord(44, "Минимальный интервал предварительной подкачки топлива (секунд)"),
            new Primitive_KeyValueRecord(41, "Длительность предварительной подкачки топлива (секунд)"),
            new Primitive_KeyValueRecord(42, "Час начала светового дня"),
            new Primitive_KeyValueRecord(43, "Час конца светового дня"),
    };
    ///=================================КОНСТАНТЫ=================================
    private final byte CONST_DIAGRAM_LINE_VOLTAGE_MAIN = 0; //Индексы линий напряжений в диаграмме
    private final byte CONST_DIAGRAM_LINE_VOLTAGE_TEMPERATURE = 1;
    private final byte CONST_DIAGRAM_LINE_VOLTAGE_FUEL = 2;
    //==================================НАСТРОЙКИ=====================================
    // TODO: 29.08.2018 Сделать нормальную вкладку с настройками в главном окне
    //Максимальное время ожидания ответа от модуля при подключении к порту
    private long MAX_CONNECTION_WAIT_TIMEOUT = 3000;
    //Максимальное время ожидания ответа от модуля при отправке какого - либо запроса
    private long RESPONSE_WAIT_TIMEOUT = 500;
    //Количество ошибок связи, после которого соединение будет принудительно перезапущено
    private int RECONNECT_ERRORS_THRESHOLD = 3;
    //Длительность показа статуса с ошибкой, после чего он будет очищен
    private long ERRONEOUS_STATUS_HOLD_TIME = 5000; //Millis
    //Длительность показа статуса с информацией, после чего он будет очищен
    private long COMMON_STATUS_HOLD_TIME = 2000; //Millis
    //Максимальное количество измерений, сохряняемое в каждую из линий диаграммы
    private int DIAGRAM_LINES_MAX_CAPACITY = 0;
    ///================================ПОЛЯ КЛАССА================================
    private BORT_connection connection; //Текущее соединение с модулем
    private MainGUIForm mainGUIForm; //Основная форма
    private volatile ActionEvent actionEvent = null; //Текущее полученное событие от формы
    private int connectionErrorsCounter = 0; //Счётчик количества ошибок в соединении
    private GraphDiagram voltageDiagram; //Диаграмма напряжений
    private GraphDiagram RPM_diagram; //Диаграмма оборотов
    private GraphDiagram temperatureDiagram; //Диаграмма температуры
    private GraphDiagram fuelLevelDiagram; //Диаграмма уровня топлива

    public static void main(String[] args) {
        new BORT_debug_manager().run();
    }

    private void run() {
        mainGUIForm = new MainGUIForm(new Dimension(800, 500), this);

        voltageDiagram = new GraphDiagram(mainGUIForm.getDiagramPanel(), " volts", 20, 200);
        RPM_diagram = new GraphDiagram(mainGUIForm.getDiagramPanel(), " RPM", 20, 100);
        temperatureDiagram = new GraphDiagram(mainGUIForm.getDiagramPanel(), " C", 20, 300);
        fuelLevelDiagram = new GraphDiagram(mainGUIForm.getDiagramPanel(), " litres", 20, 400);

        voltageDiagram.getLines().add(new GraphLine(Color.BLUE, "Main voltage", DIAGRAM_LINES_MAX_CAPACITY)); //0
        voltageDiagram.getLines().add(new GraphLine(new Color(0xAA0000), "Temperature sensor voltage", DIAGRAM_LINES_MAX_CAPACITY)); //1
        voltageDiagram.getLines().add(new GraphLine(new Color(0x007000), "Fuel sensor voltage", DIAGRAM_LINES_MAX_CAPACITY)); //2

        RPM_diagram.getLines().add(new GraphLine(Color.BLUE, "RPM", DIAGRAM_LINES_MAX_CAPACITY));

        temperatureDiagram.getLines().add(new GraphLine(new Color(0xAA0000), "Engine temperature", DIAGRAM_LINES_MAX_CAPACITY));

        fuelLevelDiagram.getLines().add(new GraphLine(new Color(0x007000), "Fuel level", DIAGRAM_LINES_MAX_CAPACITY));
        mainGUIForm.getSelectGraphComboBox().addActionListener(this);

        connection = findAndConnectToTheModule();
        checkRTCparams();
        try {
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
                delayMs(200);
                try {
                    requestStatistics();
                    if (actionEvent != null) {
                        interpretateAction();
                    }
                    connectionErrorsCounter = 0;
                    if (System.currentTimeMillis() - mainGUIForm.getLastStatusUpdateTime() > (mainGUIForm.isLastStatusWasErroneous() ? ERRONEOUS_STATUS_HOLD_TIME : COMMON_STATUS_HOLD_TIME)) {
                        mainGUIForm.updateStatus("");
                    }
                } catch (InterruptedException e) {
                    mainGUIForm.updateStatus("Операция прервана");
                } catch (TimeoutException e) {
                    connectionErrorsCounter++;
                    mainGUIForm.updateErrorStatus(CONST_STR_DATA_RECEIVING_TIMEOUT);
                } catch (InvalidParameterException e) {
                    mainGUIForm.updateErrorStatus("Неизвестная команда: " + e.getMessage());
                }
                mainGUIForm.getProgressBar().setVisible(false);
                actionEvent = null;
            }
        } catch (Exception anyUncatchedCriticalException) {
            if (mainGUIForm.isShowing()) {
                mainGUIForm.updateErrorStatus("CRITICAL ERROR: " + anyUncatchedCriticalException);
            }
            System.err.println("CRITICAL ERROR (UNCATCHED): " + anyUncatchedCriticalException.toString());
        }
    }

    private void interpretateAction() throws TimeoutException, InterruptedException {
        // TODO: 29.08.2018 Вынести все литералы в константы и в отдельный класс. Смысл есть: его использует и класс - GUI
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
            case "clear diagram":
                clearDiagram();
                break;
            default:
                throw new InvalidParameterException(actionEvent.getActionCommand().toLowerCase().trim());
        }
    }

    private void clearDiagram() {
        switch (mainGUIForm.getSelectGraphComboBox().getSelectedIndex()) {
            case MainGUIForm.CHOSEN_DIAGRAMM_TYPE_VOLTAGE:
                voltageDiagram.getLines().get(CONST_DIAGRAM_LINE_VOLTAGE_MAIN).getValues().clear();
                voltageDiagram.getLines().get(CONST_DIAGRAM_LINE_VOLTAGE_FUEL).getValues().clear();
                voltageDiagram.getLines().get(CONST_DIAGRAM_LINE_VOLTAGE_TEMPERATURE).getValues().clear();
                break;
            case MainGUIForm.CHOSEN_DIAGRAMM_TYPE_RPM:
                RPM_diagram.getLines().get(0).getValues().clear();
                break;
            case MainGUIForm.CHOSEN_DIAGRAMM_TYPE_TEMPERATURE:
                temperatureDiagram.getLines().get(0).getValues().clear();
                break;
            case MainGUIForm.CHOSEN_DIAGRAMM_TYPE_FUEL_LEVEL:
                fuelLevelDiagram.getLines().get(0).getValues().clear();
        }
    }

    private void updateParamsList() throws InterruptedException {
        try {
            int lastIndex = mainGUIForm.getParametersListBox().getSelectedIndex();
            mainGUIForm.getParametersListBox().setEnabled(false);
            mainGUIForm.getSelectedParameterComboBox().setEnabled(false);
            mainGUIForm.setParamsList(readAllParametersFromModule(false));
            mainGUIForm.getParametersListBox().setSelectedIndex(lastIndex);
            mainGUIForm.getSelectedParameterComboBox().setSelectedIndex(lastIndex);
        } catch (IllegalArgumentException e) {
            mainGUIForm.updateErrorStatus("Внутренняя ошибка #471: ошибка назначения индекса при выборе параметра.");
        }
    }

    private void checkRTCparams() {
        mainGUIForm.updateStatus("Проверка корректности установки времени...");
        try {
            isTimeOnRTCsetted();
            setTimeOnRTC();
            mainGUIForm.updateStatus("Время синхронизировано успешно");
        } catch (InterruptedException ignored) {
        } catch (TimeoutException e) {
            mainGUIForm.updateStatus(CONST_STR_DATA_RECEIVING_TIMEOUT);
            connectionErrorsCounter++;
        }
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
            connection.send(CommandsConstList.CMD_READ_TIME_FROM_RTC);
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
        // TODO: 20.10.2018 ПОСЛЕ ОБНОВЛЕНИЯ ПРОШИВКИ РАСКОММЕНТИРОВАТЬ!
        //String currentTime = new SimpleDateFormat("dd-MM-yyyy;HH:mm:ss").format(new Date());
        mainGUIForm.updateStatus("Устанавливаем текущее время в RTC...");
        connection.send(CommandsConstList.CMD_SET + "32=" + currentTime);
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
        connection.send(CommandsConstList.CMD_RESTART_MODULE);
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
        connection.send(CommandsConstList.CMD_SET + paramId + '=' + value);
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
                connection.send(CommandsConstList.CMD_SET + getParamIndexFromParamName(currentParameter.getKey()) + '=' + currentParameter.getValue());
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
                connection.send(CommandsConstList.CMD_READ_PARAM + currentParam.key + '=');
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
        if (!allIsSuccessful) {
            if (notifyIfNotSucessful && new AreYouSureDialogProcessor().showDialog("Возникли проблемы при чтении некоторых параметров. Вы хотите продолжить?") != ApiDialog.DialogResult.OK) {
                throw new InterruptedException("Ошибка: не удалось получить все параметры. Дальнейшие действия отменены.");
            }
            mainGUIForm.updateErrorStatus("Ошибка: не удалось получить некоторые параметры.");
        } else {
            mainGUIForm.updateStatus("Все параметры были получены");
        }
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
        connection.send(CommandsConstList.CMD_SET + "38=1");
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
        connection.send(CommandsConstList.CMD_SAVE_SETTINGS_TO_EEPROM);
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
        connection.send(CommandsConstList.CMD_REQUEST_STATISTICS);
        //mainGUIForm.updateStatus("Запрашиваем статистику...");
        BORT_responseType waitingResponseTypes[] = {BORT_responseType.INFO_STATISTICS};
        BORT_response gettedResponse = waitForIncomingResponse(waitingResponseTypes, RESPONSE_WAIT_TIMEOUT);

        updateDiagrammsInfo(gettedResponse);
        mainGUIForm.updateStatistics(gettedResponse.toString());
    }

    private void updateDiagrammsInfo(BORT_response gettedResponse) {
        voltageDiagram.setRenderValuesAmount(mainGUIForm.getGraphSizeSlider().getValue());
        RPM_diagram.setRenderValuesAmount(mainGUIForm.getGraphSizeSlider().getValue());
        temperatureDiagram.setRenderValuesAmount(mainGUIForm.getGraphSizeSlider().getValue());
        fuelLevelDiagram.setRenderValuesAmount(mainGUIForm.getGraphSizeSlider().getValue());
        try {
            voltageDiagram.getLines().get(CONST_DIAGRAM_LINE_VOLTAGE_MAIN).getValues().add(Double.valueOf(gettedResponse.getResponseParams().getFieldByKey(BORT_response.CONST_STATISTICS_VOLTAGE_MAIN).getValue()));
            voltageDiagram.getLines().get(CONST_DIAGRAM_LINE_VOLTAGE_TEMPERATURE).getValues().add(Double.valueOf(gettedResponse.getResponseParams().getFieldByKey(BORT_response.CONST_STATISTICS_VOLTAGE_TEMPERATURE).getValue()));
            voltageDiagram.getLines().get(CONST_DIAGRAM_LINE_VOLTAGE_FUEL).getValues().add(Double.valueOf(gettedResponse.getResponseParams().getFieldByKey(BORT_response.CONST_STATISTICS_VOLTAGE_FUEL).getValue()));

            RPM_diagram.getLines().get(0).getValues().add(Double.valueOf(gettedResponse.getResponseParams().getFieldByKey(BORT_response.CONST_STATISTICS_RPM).getValue()));
            temperatureDiagram.getLines().get(0).getValues().add(Double.valueOf(gettedResponse.getResponseParams().getFieldByKey(BORT_response.CONST_STATISTICS_ENGINE_TEMPERATURE).getValue()));
            fuelLevelDiagram.getLines().get(0).getValues().add(Double.valueOf(gettedResponse.getResponseParams().getFieldByKey(BORT_response.CONST_STATISTICS_FUEL_LEVEL).getValue()));

        } catch (NotFoundException e) {
            mainGUIForm.updateErrorStatus("Ошибка во время обновления диаграммы: входные статистические данные некорректны.");
        }
        drawSelectedDiagram();
    }

    private void drawSelectedDiagram() {
        switch (mainGUIForm.getSelectGraphComboBox().getSelectedIndex()) {
            case MainGUIForm.CHOSEN_DIAGRAMM_TYPE_VOLTAGE:
                voltageDiagram.setRedrawIfResized(true); // ←
                RPM_diagram.setRedrawIfResized(false);
                temperatureDiagram.setRedrawIfResized(false);
                fuelLevelDiagram.setRedrawIfResized(false);

                voltageDiagram.draw();
                break;
            case MainGUIForm.CHOSEN_DIAGRAMM_TYPE_RPM:
                voltageDiagram.setRedrawIfResized(false);
                RPM_diagram.setRedrawIfResized(true);// ←
                temperatureDiagram.setRedrawIfResized(false);
                fuelLevelDiagram.setRedrawIfResized(false);

                RPM_diagram.draw();
                break;
            case MainGUIForm.CHOSEN_DIAGRAMM_TYPE_TEMPERATURE:
                voltageDiagram.setRedrawIfResized(false);
                RPM_diagram.setRedrawIfResized(false);
                temperatureDiagram.setRedrawIfResized(true);// ←
                fuelLevelDiagram.setRedrawIfResized(false);

                temperatureDiagram.draw();
                break;
            case MainGUIForm.CHOSEN_DIAGRAMM_TYPE_FUEL_LEVEL:
                voltageDiagram.setRedrawIfResized(false);
                RPM_diagram.setRedrawIfResized(false);
                temperatureDiagram.setRedrawIfResized(false);
                fuelLevelDiagram.setRedrawIfResized(true);// ←

                fuelLevelDiagram.draw();
        }
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
            try {
                for (BORT_response currentResponse : connection.getResponsesStack()) {
                    for (BORT_responseType currentResponseType : responseTypes) {
                        if (currentResponse.getResponseType() == currentResponseType) {
                            return currentResponse;
                        }
                    }
                }
            } catch (ConcurrentModificationException ignored) { //Если мы получили ответ в процессе перебора

            }
            delayMs(20);
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
            if (SerialPortList.getPortNames().length > 0 && mainGUIForm.getHoldConnectionCheckBox().isSelected()) {
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
            delayMs(100);
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
        if (actionEvent.getSource() == mainGUIForm.getSelectGraphComboBox() || actionEvent.getActionCommand().equalsIgnoreCase("refresh diagram")) {
            drawSelectedDiagram();
        } else {
            this.actionEvent = actionEvent;
        }
    }

    @Override
    public void debugInformationReceive(BORT_response response) {
        String currentTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
        mainGUIForm.updateDebugStatusBar(currentTime + " | " + response.toString() + '\n');
    }

    @Override
    public void errorMessageNotifyReceive(String message) {
        mainGUIForm.updateErrorStatus(message);
    }
}
