import classes.BORT_connection;
import classes.BORT_response;
import classes.BORT_responseType;
import classes.Primitive_KeyValueRecord;
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
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

/**
 * Created by Кирилл on 10.06.2018.
 */
@SuppressWarnings("FieldCanBeLocal")
class BORT_debug_manager implements ActionListener {

    private final long MAX_CONNECTION_WAIT_TIMEOUT = 3000;
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
        while (connection.getSerialPort().isOpened()) {
            if (connectionErrorsCounter >= RECONNECT_ERRORS_THRESHOLD) {
                mainGUIForm.updateStatus("Соединение потеряно. Переподключаемся...");
                connection.close();
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
                updateParamsList();
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
        mainGUIForm.setParamsList(readAllParametersFromModule(false).getSections().get(0));

        /*mainGUIForm.getTable1().removeAll();
        mainGUIForm.getTable1().addColumn(new TableColumn());*/
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

    /*private void runMenu() {

        while (connection.getSerialPort().isOpened()) {
            if (connectionErrorsCounter >= RECONNECT_ERRORS_THRESHOLD) {
                mainGUIForm.updateErrorStatus("Соединение потеряно. Переподключаемся...");
                closeConnection(false);
                connection = findAndConnectToTheModule();
                connectionErrorsCounter = 0;
            }
            mainGUIForm.updateStatus("\n\n\n\nВыберите действие:");
            mainGUIForm.updateStatus("set - установить значение параметра");
            mainGUIForm.updateStatus("restart - перезагрузить бортовой компьютер");
            mainGUIForm.updateStatus("time - установить текущее время на RTC");
            mainGUIForm.updateStatus("export - прочесть все настройки бортовика и по желанию сохранить в ini - файле");
            mainGUIForm.updateStatus("import - загрузить настройки бортовика из ini - файла, и поместить в бортовик");
            mainGUIForm.updateStatus("save - сохранить все изменённые настройки в энергонезависимую память");
            mainGUIForm.updateStatus("reset EP - сбросить все настройки бортовика на настройки по умолчанию.");
            mainGUIForm.updateStatus("info или просто нажмите enter - показать текущую статистику");
            mainGUIForm.updateStatus("reconnect - переподключиться");
            try {
                switch(getEnteredString("_>").toLowerCase().trim()) {
                    case "set":
                        setParameterDialog();
                        break;
                    case "restart":
                        restartModule();
                        break;
                    case "export":
                        exportModuleSettingsToFile();
                        break;
                    case "import":
                        importParamsIntoModule();
                        break;
                    case "time":
                        setTimeOnRTC();
                        break;
                    case "reset ep":
                        reset_EEPROM();
                        break;
                    case "info":
                    case "":
                        requestStatistics();
                        break;
                    case "save":
                        saveSettingsToEEPROM();
                        break;
                    case "reconnect":
                        connection.close();
                        connection = findAndConnectToTheModule();
                        break;
                    default:
                        throw new InvalidParameterException();
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
            getEnteredString("Чтобы перейти к меню, нажмите enter");
        }
    }*/


    private void exportModuleSettingsToFile() throws InterruptedException {
        try {
            String gettedFileName = "";
            INISettings readedParameters = readAllParametersFromModule(true);
            JFileChooser fileopen = new JFileChooser();
            fileopen.setCurrentDirectory(new File("."));
            int ret = fileopen.showDialog(null, "Выбрать файл для сохранения настроек");
            if (ret == JFileChooser.APPROVE_OPTION) {
                gettedFileName = fileopen.getSelectedFile().getAbsolutePath();
            } else {
                throw new InterruptedException();
            }

           /* String gettedFileName = new InputStringDialogProcessor().showDialog("Введите путь до файла, в который вы хотите сохранить параметры, или нажмите enter, если вы не хотите её сохранять...\n_>");
            if (gettedFileName.equals("")) {
                throw new InterruptedException();
            }*/
            mainGUIForm.updateStatus("Сохраняем настройки в файл...");
            readedParameters.saveToFile(gettedFileName);
            mainGUIForm.updateStatus("Настройки успешно сохранены!");
        } catch (UnexpectedException e) {
            mainGUIForm.updateErrorStatus(e.getMessage());
        } catch (IOException e) {
            mainGUIForm.updateErrorStatus("Ошибка ввода-вывода во время записи в файл: " + e.getMessage());
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
        Date dateNow = new Date();
        SimpleDateFormat formatForDateNow = new SimpleDateFormat("HH:mm:ss");
        String currentTime = formatForDateNow.format(dateNow);
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
           /* connection.send("$8:");
            BORT_responseType new_waitingResponseTypes[] = {BORT_responseType.ERR_RTC_CONNECTION_FAILED, BORT_responseType.ERR_RTC_UNSETTED, BORT_responseType.RTC_TIME_READ_SUCCESS};
            BORT_response new_gettedResponse = waitForIncomingResponse(new_waitingResponseTypes, RESPONSE_WAIT_TIMEOUT);
            if (new_gettedResponse.getResponseType() == BORT_responseType.RTC_TIME_READ_SUCCESS || new_gettedResponse.getResponseParams().getRecords().size() > 0) {
                mainGUIForm.updateStatus("Текущее время по RTC: " + new_gettedResponse.getResponseParams().getFieldByKey("RTC").getValue());
            } else {
                mainGUIForm.updateErrorStatus(gettedResponse);
            }*/
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
        //printListOfAvailableParameters();
        /*try {
            readAllParametersFromModule();
        } catch (UnexpectedException e) {
            mainGUIForm.updateErrorStatus(e.getMessage());
            throw new InterruptedException();
        }*/
        // boolean success = false;
        //do {
        try {
           /* String gettedStr = getEnteredString("Введите название параметра, или нажмите enter для выхода...\n_>");
            if (gettedStr.equals("")) {
                throw new InterruptedException();
            }*/
            int paramIndex = getParamIndexFromParamName((String) mainGUIForm.getSelectedParameterComboBox().getSelectedItem());
            //readParameter(paramIndex);
            // gettedStr = getEnteredString("Введите новое значение параметра, или нажмите enter для выхода...\n_>");
            if (mainGUIForm.getSelectedParamNewValueField().getText().equals("")) {
                mainGUIForm.updateErrorStatus("Новое значение параметра не может быть пустым");
            } else {
                writeParameter(paramIndex, mainGUIForm.getSelectedParamNewValueField().getText().trim());
                updateParamsList();
            }
            //success = true;
        } catch (NotFoundException e) {
            mainGUIForm.updateErrorStatus("Параметра с данным названием не существует.");
        }
        // } while (!success);
    }

    private void readParameter(int paramId) throws TimeoutException {
        connection.send("$3:" + paramId + '=');
        mainGUIForm.updateStatus("Читаем значение параметра...");
        BORT_responseType waitingResponseTypes[] = {BORT_responseType.ERR_WRONG_PARAM_NAME, BORT_responseType.PARAM_RW_SUCCESS};
        BORT_response gettedResponse = waitForIncomingResponse(waitingResponseTypes, RESPONSE_WAIT_TIMEOUT);
        if (gettedResponse.getResponseType() != BORT_responseType.ERR_WRONG_PARAM_NAME && gettedResponse.getResponseParams().getRecords().size() > 0) {
            mainGUIForm.updateStatus("Ответ получен. Текущее значение параметра: \"" + gettedResponse.getResponseParams().getRecords().get(0).getValue() + '"');
        } else {
            mainGUIForm.updateErrorStatus(gettedResponse.toString());
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

    private void printListOfAvailableParameters() {
        mainGUIForm.updateStatus("Список доступных параметров: ");
        for (Primitive_KeyValueRecord currentParam : MODULE_PARAMETERS_LIST) {
            mainGUIForm.updateStatus(currentParam.value);
        }
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
            writeAllParametersIntoModule(loadedSettings.getSections().get(0));
            saveSettingsToEEPROM();
            restartModule();
            mainGUIForm.updateStatus("Операция выполнена");
        } catch (IOException e) {
            mainGUIForm.updateErrorStatus("Ошибка ввода - вывода при чтении из файла: " + e.getMessage());
        } catch (IniSettingsException e) {
            mainGUIForm.updateErrorStatus("Ошибка при обработке файла: " + e.getMessage());
        } catch (IndexOutOfBoundsException e) {
            mainGUIForm.updateErrorStatus("Ошибка при обработке файла - ошибка поиска ключа: " + e.getMessage());
        }
    }

    private void writeAllParametersIntoModule(INISettingsSection parameters) {
        mainGUIForm.updateStatus("Записываем настройки в модуль...");
        //System.out.print(getPercentLine(0, 60));
        int paramsWritedCounter = 0;
        boolean allWritedSucessfully = true;
        mainGUIForm.getProgressBar().setVisible(true);
        mainGUIForm.getProgressBar().setMaximum(parameters.getRecords().size());
        mainGUIForm.getProgressBar().setMinimum(0);
        for (INISettingsRecord currentParameter : parameters.getRecords()) {
            try {
                mainGUIForm.getProgressBar().setValue(++paramsWritedCounter);
                mainGUIForm.updateStatus("Запись " + paramsWritedCounter + " из " + parameters.getRecords().size() + " - " + currentParameter.getKey() + "...");
                //System.out.print('\r' + getPercentLine(paramsWritedCounter++ * 100 / parameters.getRecords().size(), 60) + " (" + currentParameter.getKey() + ')');
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
     * Возвращает строку ASCII - графики, представляющую собой progressBar.
     *
     * @param percents процентное соотношение заполненной части шкалы к пустой.
     * @return ASCII - строка, представляющая собой progressBar.
     */
    private String getPercentLine(int percents, int length) {
        StringBuilder temp = new StringBuilder("[");
        percents /= 100. / length;
        for (int i = 1; i <= length; i++) {
            if (i <= percents) {
                temp.append('=');
            } else {
                temp.append(' ');
            }
        }
        temp.append("] | ");
        temp.append(new DecimalFormat("#0.00").format(percents * 100. / length));
        temp.append('%');
        return temp.toString();
    }


    private INISettings readAllParametersFromModule(boolean notifyIfNotSucessful) throws InterruptedException {
        INISettings result = new INISettings();
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
                //System.out.print(currentParam.value + "... ");
                mainGUIForm.getProgressBar().setValue(++counter);
                mainGUIForm.updateStatus("Запрос параметра " + counter + " из " + MODULE_PARAMETERS_LIST.length + " - " + currentParam.value + "...");
                try {
                    BORT_responseType waitingResponseTypes[] = {BORT_responseType.ERR_WRONG_PARAM_NAME, BORT_responseType.PARAM_RW_SUCCESS};
                    BORT_response gettedResponse = waitForIncomingResponse(waitingResponseTypes, RESPONSE_WAIT_TIMEOUT);
                    if (gettedResponse.getResponseType() != BORT_responseType.ERR_WRONG_PARAM_NAME && gettedResponse.getResponseParams().getRecords().size() > 0) {
                        //mainGUIForm.updateStatus('\r' + currentParam.value + " = " + gettedResponse.getResponseParams().getRecords().get(0).getValue());
                        readedParamsList.addField(new INISettingsRecord(currentParam.value, gettedResponse.getResponseParams().getRecords().get(0).getValue()));
                    } else {
                        allIsSuccessful = false;
                        //mainGUIForm.updateStatus("\r" + currentParam.value + " = " + gettedResponse);
                    }
                } catch (TimeoutException e) {
                    allIsSuccessful = false;
                    mainGUIForm.updateStatus(currentParam.value + " - ошибка. Таймаут.");
                }
            }
            result.addSection(readedParamsList);
        } catch (AlreadyExistsException ignored) {
        }
        if (!allIsSuccessful && notifyIfNotSucessful) {
            if (new AreYouSureDialogProcessor().showDialog("Возникли проблемы при чтении некоторых параметров. Вы хотите продолжить?") != ApiDialog.DialogResult.OK) {
                throw new InterruptedException("Ошибка: не удалось получить все параметры. Дальнейшие действия отменены.");
            }
        }
        mainGUIForm.updateStatus("Все параметры были получены");
        return result;
    }

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
    }

    private void saveSettingsToEEPROM() throws TimeoutException {
        mainGUIForm.updateStatus("Отправляем запрос на сохранение...");
        connection.send("$4:");
        BORT_responseType waitingResponseTypes[] = {BORT_responseType.EEPROM_SAVE_CMD_GETTED};
        BORT_response gettedResponse = waitForIncomingResponse(waitingResponseTypes, RESPONSE_WAIT_TIMEOUT);
        mainGUIForm.updateStatus(gettedResponse.toString());
    }

    private void requestStatistics() throws TimeoutException {
        connection.send("$7:");
        //mainGUIForm.updateStatus("Запрашиваем статистику...");
        BORT_responseType waitingResponseTypes[] = {BORT_responseType.INFO_STATISTICS};
        BORT_response gettedResponse = waitForIncomingResponse(waitingResponseTypes, RESPONSE_WAIT_TIMEOUT);
        mainGUIForm.updateStatistics(gettedResponse.toString());
    }

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
                    BORT_connection = new BORT_connection(selectedPort);
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
     * Выводит пользователю пояняющее сообщение, и возвращает введённую им строку.
     *
     * @param message Сообщение, которое будет выведено.
     * @return Введённая пользователем строка
     */
    public String getEnteredString(String message) {
        System.out.print(message);
        return new Scanner(System.in).nextLine();
    }

    /**
     * Выводит пользователю пояняющее сообщение, и возвращает введённое им целое число.
     *
     * @param message Сообщение, которое будет выведено.
     * @return Введённое пользователем число.
     */
    public int getEnteredIntegerNumber(String message) {
        System.out.print(message);
        for (; ; ) {
            try {
                return new Scanner(System.in).nextInt();
            } catch (InputMismatchException e) {
                mainGUIForm.updateErrorStatus("Ошибка ввода, повторите попытку...");
            }
        }
    }

    /**
     * Invoked when an action occurs.
     *
     * @param actionEvent
     */
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        this.actionEvent = actionEvent;
    }
}
