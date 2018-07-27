package classes;

import exceptions.InterpretationException;
import iniSettings.INISettingsRecord;
import iniSettings.INISettingsSection;
import iniSettings.exceptions.AlreadyExistsException;
import iniSettings.exceptions.IniSettingsException;
import iniSettings.exceptions.NotFoundException;
import iniSettings.exceptions.RecordParsingException;

import java.util.ArrayList;

/**
 * Класс, представляющий ответ от модуля в виде объекта.
 * Имеет такие свойства, как тип ответа в виде объекта BORT_responseType и параметры ответа в виде секции ini - файла,
 * выраженной в форме объекта INISettingsSection
 */
public class BORT_response {
    private static final String STATISTICS_VOLTAGE_MAIN = "VM_L";
    private static final String STATISTICS_VOLTAGE_TEMPERATURE = "VM_T";
    private static final String STATISTICS_VOLTAGE_FUEL = "VM_F";
    private static final String STATISTICS_RPM = "RPM";
    private static final String STATISTICS_ACCESSORIES = "ACC";
    private static final String STATISTICS_IGNITION = "IGN";
    private static final String STATISTICS_PARKING_LIGHTS = "PLT";
    private static final String STATISTICS_HANDBRAKE = "HB";
    private static final String STATISTICS_TURNERS_SIGNAL = "TS";
    private static final String STATISTICS_TIME = "TIME";
    private static final String STATISTICS_I2C_ERRORS = "CER";
    private static final String STATISTICS_CRANKSHAFT_TURNS = "MC";
    private static final String STATISTICS_MASHINE_HOURS = "MHR";
    private static final String STATISTICS_STATUSBAR_CAPTION = "SBC";
    private static final String STATISTICS_FUEL_LEVEL = "FL";
    private static final String STATISTICS_ENGINE_TEMPERATURE = "TP";
    private static final String STATISTICS_TIME_FROM_STARTUP = "ST";
    private static final String STATISTICS_ENGINE_STOP_TIME = "EST";
    private final INISettingsSection responseParams = new INISettingsSection("BORT_RESPONSE");
    private BORT_responseType responseType;

    BORT_response(String responseStr) throws InterpretationException {
        if (!responseStr.contains("!") || !responseStr.contains(":")) {
            throw new InterpretationException("Input string aren't valid");
        }
        int responseCode;
        try {
            responseCode = Integer.valueOf(responseStr.substring(responseStr.indexOf('!') + 1, responseStr.indexOf(':')));
        } catch (NumberFormatException e) {
            throw new InterpretationException("Response code extraction failure - wrong number format");
        } catch (StringIndexOutOfBoundsException e) {
            throw new InterpretationException("Response code extraction failure - invalid index");
        }
        responseType = BORT_responseType.getResponseTypeFromNumericalCode(responseCode);
        try {
            addParamsIntoSection(extractParamsFromResponse(responseStr));
        } catch (IniSettingsException e) {
            throw new InterpretationException("Params parsing error");
        }
    }

    private ArrayList<String> extractParamsFromResponse(String responseStr) {
        ArrayList<String> result = new ArrayList<>();
        if (responseStr.indexOf(':') < responseStr.length()) {
            result = splitText(responseStr.substring(responseStr.indexOf(':') + 1), ";");
        }
        return result;
    }

    public BORT_responseType getResponseType() {
        return responseType;
    }

    public INISettingsSection getResponseParams() {
        return responseParams;
    }

    private void addParamsIntoSection(ArrayList<String> input) throws RecordParsingException {
        for (String currentParam : input) {
            if (currentParam.contains("=")) {
                try {
                    responseParams.addField(new INISettingsRecord(currentParam));
                } catch (AlreadyExistsException ignored) {
                }
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private ArrayList<String> splitText(String text, String delimiter) {
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder builder = new StringBuilder(text);
        while (builder.indexOf(delimiter) != -1) {
            lines.add(builder.substring(0, builder.indexOf(delimiter)));
            builder.delete(0, builder.indexOf(delimiter) + 1);
        }
        lines.add(builder.toString());
        return lines;
    }

    @Override
    public String toString() {
        switch (responseType) {
            case ECHO:
                return "ECHO: " + responseParams;
            case DEBUG_INFORMATION:
                try {
                    return "Debug information: " + responseParams.getFieldByKey("DBG").getValue();
                } catch (NotFoundException ignored) {
                    return "Command params error";
                }
            case PARAM_RW_SUCCESS:
                return "Команда чтения/записи обработана успешно";
            case EEPROM_RESET_SUCCESS:
                return "Флаг сброса настроек на настройки по умолчанию установлен. Настройки будут сброшены после перезагрузки модуля...";
            case RESTART_CMD_GETTED:
                return "Команда перезагрузки получена. Устройство выполняет перезагрузку.";
            case WRITE_CMD_GETTED:
                if (responseParams.getRecords().size() > 0) {
                    return "Установка значения успешно произведена. Текущее значение: \"" + responseParams.getRecords().get(0).getValue() + '"';
                } else {
                    return "Установка значения успешно произведена, однако модуль не предоставил новое значение параметра.";
                }
            case READ_CMD_GETTED:
                if (responseParams.getRecords().size() > 0) {
                    return "Запрос на чтение обработан. Текущее значение параметра: \"" + responseParams.getRecords().get(0).getValue() + '"';
                } else {
                    return "Запрос на чтение обработан, однако модуль не предоставил значение параметра";
                }
            case EEPROM_SAVE_CMD_GETTED:
                return "Настройки сохранены в энергонезависимую память";
            case EEPROM_LOAD_CMD_GETTED:
                return "Настройки загружены из энергонезависимой памяти";
            case ERR_WRONG_PARAM_NAME:
                return "Ошибка связи: неверное название параметра";
            case ERR_RTC_CONNECTION_FAILED:
                return "Ошибка: проблемы со связью в RTC";
            case ERR_RTC_UNSETTED:
                return "Ошибка: время на RTC не установлено";
            case ERR_WRONG_CMD_TYPE:
                return "Ошибка связи: неверно указан тип";
            case INFO_STATISTICS:
                StringBuilder result = new StringBuilder();
                try {
                    result.append("Напряжение в бортовой линии: ");
                    result.append(responseParams.getFieldByKey(STATISTICS_VOLTAGE_MAIN).getValue());
                    result.append(" вольт\n");
                    result.append("Напряжение на датчике температуры: ");
                    result.append(responseParams.getFieldByKey(STATISTICS_VOLTAGE_TEMPERATURE).getValue());
                    result.append(" вольт; Коэффициент уровня: ");
                    result.append(roundTo((Double.valueOf(responseParams.getFieldByKey(STATISTICS_VOLTAGE_TEMPERATURE).getValue()) * 100) / Double.valueOf(responseParams.getFieldByKey(STATISTICS_VOLTAGE_MAIN).getValue()), 3));
                    result.append('\n');
                    result.append("Напряжение на датчике уровня топлива: ");
                    result.append(responseParams.getFieldByKey(STATISTICS_VOLTAGE_FUEL).getValue());
                    result.append(" вольт; Коэффициент уровня: ");
                    result.append(roundTo((Double.valueOf(responseParams.getFieldByKey(STATISTICS_VOLTAGE_FUEL).getValue()) * 100) / Double.valueOf(responseParams.getFieldByKey(STATISTICS_VOLTAGE_MAIN).getValue()), 3));
                    result.append('\n');
                    result.append("Показания тахометра: ");
                    result.append(responseParams.getFieldByKey(STATISTICS_RPM).getValue());
                    result.append(" об/мин\n");
                    result.append("Состояние линии аксессуаров: ");
                    result.append(responseParams.getFieldByKey(STATISTICS_ACCESSORIES).getValue().equals("0") ? "выключено (OFF)" : "включено (ON)");
                    result.append('\n');
                    result.append("Состояние линии зажигания: ");
                    result.append(responseParams.getFieldByKey(STATISTICS_IGNITION).getValue().equals("0") ? "выключено (OFF)" : "включено (ON)");
                    result.append('\n');
                    result.append("Состояние линии габаритных огней: ");
                    result.append(responseParams.getFieldByKey(STATISTICS_PARKING_LIGHTS).getValue().equals("0") ? "выключено (OFF)" : "включено (ON)");
                    result.append('\n');
                    result.append("Состояние линии ручника: ");
                    result.append(responseParams.getFieldByKey(STATISTICS_HANDBRAKE).getValue().equals("0") ? "выключено (OFF)" : "включено (ON)");
                    result.append('\n');
                    result.append("Состояние линии сигнала поворотников: ");
                    result.append(responseParams.getFieldByKey(STATISTICS_TURNERS_SIGNAL).getValue().equals("0") ? "выключено (OFF)" : "включено (ON)");
                    result.append('\n');
                    result.append("Время с RTC: ");
                    result.append(responseParams.getFieldByKey(STATISTICS_TIME).getValue());
                    result.append('\n');
                    result.append("Количество ошибок связи по I2C: ");
                    result.append(responseParams.getFieldByKey(STATISTICS_I2C_ERRORS).getValue());
                    result.append('\n');
                    result.append("Количество накопленных оборотов коленчатого вала: ");
                    result.append(responseParams.getFieldByKey(STATISTICS_CRANKSHAFT_TURNS).getValue());
                    result.append('\n');
                    result.append("Количество мото-часов: ");
                    result.append(responseParams.getFieldByKey(STATISTICS_MASHINE_HOURS).getValue());
                    result.append('\n');
                    result.append("Текущая надпись в строке статуса: ");
                    result.append(responseParams.getFieldByKey(STATISTICS_STATUSBAR_CAPTION).getValue());
                    result.append('\n');
                    result.append("Уровень топлива: ");
                    result.append(responseParams.getFieldByKey(STATISTICS_FUEL_LEVEL).getValue());
                    result.append(" л.\n");
                    result.append("Температура двигателя: ");
                    result.append(responseParams.getFieldByKey(STATISTICS_ENGINE_TEMPERATURE).getValue());
                    result.append(" °C\n");
                    result.append("Время работы с момента включения: ");
                    result.append(formatTime(Integer.valueOf(responseParams.getFieldByKey(STATISTICS_TIME_FROM_STARTUP).getValue())));
                    result.append('\n');
                    if (!responseParams.getFieldByKey(STATISTICS_ENGINE_STOP_TIME).getValue().equals("0")) {
                        result.append("Времени прошло с остановки двигателя: ");
                        result.append(formatTime(Integer.valueOf(responseParams.getFieldByKey(STATISTICS_TIME_FROM_STARTUP).getValue()) - Integer.valueOf(responseParams.getFieldByKey(STATISTICS_ENGINE_STOP_TIME).getValue())));
                        result.append('\n');
                    }
                } catch (NotFoundException e) {
                    return "Ошибка во время обработки статистики.";
                }
                return "Текущая статистика:\n" + result;
            default:
                return "Неизвестный ответ";
        }
    }

    private String formatTime(long millis) {
        int seconds = (int) millis / 1000;
        // millis -= seconds * 1000;
        int minutes = seconds / 60;
        seconds -= minutes * 60;
        int hours = minutes / 60;
        minutes -= hours * 60;
        int days = hours / 24;
        hours -= days * 24;
        return (days > 0 ? days + " дн. " : "") + (hours > 0 ? hours + " час. " : "") + (minutes > 0 ? minutes + " мин. " : "") + seconds /*+ (false ? '.' + millis : "") */ + " сек.";
    }

   /* private double trunc(double input, int digitsAfterPoint) {
        long decimalMLT = (long) Math.pow(10, digitsAfterPoint);
        long multipliedResult = (long) (decimalMLT * input);
        return multipliedResult / (double) decimalMLT;
    }*/

    // TODO: 23.07.2018 Допили
    @SuppressWarnings("SameParameterValue")
    private double roundTo(double input, int digitsAfterPoint) {
        /*int countOfDigits = 0;
        double copyOfInput = input;
        while ((copyOfInput /= 10.) > 0) {
         countOfDigits++;
        }*/
        long decimalMLT = (long) Math.pow(10, digitsAfterPoint);
        long multipliedResult = Math.round((decimalMLT * input));
        return multipliedResult / (double) decimalMLT;
    }
}
