package classes;

import exceptions.InterpretationException;
import iniSettings.INISettingsRecord;
import iniSettings.INISettingsSection;
import iniSettings.exceptions.AlreadyExistsException;
import iniSettings.exceptions.IniSettingsException;
import iniSettings.exceptions.NotFoundException;
import iniSettings.exceptions.RecordParsingException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * Created by Кирилл on 10.06.2018.
 */
public class BORT_response {
    private BORT_responseType responseType;
    private final INISettingsSection responseParams = new INISettingsSection("BORT_RESPONSE");

    public BORT_response(String responseStr) throws InterpretationException {
        if (!responseStr.contains("!") || !responseStr.contains(":")) {
            throw new InterpretationException("Input string aren't valid");
        }
        int responseCode;
        try {
            responseCode = Integer.valueOf(responseStr.substring(responseStr.indexOf('!') + 1, responseStr.indexOf(':')));
        } catch (NumberFormatException e) {
            throw new InterpretationException("Response code extraction failure");
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
        ArrayList<INISettingsRecord> result = new ArrayList<>(input.size());
        for (String currentParam : input) {
            if (currentParam.contains("=")) {
                try {
                    responseParams.addField(new INISettingsRecord(currentParam));
                } catch (AlreadyExistsException ignored) {}
            }
        }
    }

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
                    result.append("Напряжение в бортовой линии: "); result.append(responseParams.getFieldByKey("VM_L").getValue()); result.append(" вольт\n");
                    result.append("Напряжение на датчике температуры: "); result.append(responseParams.getFieldByKey("VM_T").getValue()); result.append(" вольт\n");
                    result.append("Напряжение на датчике уровня топлива: "); result.append(responseParams.getFieldByKey("VM_F").getValue()); result.append(" вольт\n");
                    result.append("Показания тахометра: "); result.append(responseParams.getFieldByKey("RPM").getValue()); result.append(" об/мин\n");
                    result.append("Состояние линии аксессуаров: "); result.append(responseParams.getFieldByKey("ACC").getValue().equals("0") ? "выключено (OFF)" : "включено (ON)"); result.append('\n');
                    result.append("Состояние линии зажигания: "); result.append(responseParams.getFieldByKey("IGN").getValue().equals("0") ? "выключено (OFF)" : "включено (ON)"); result.append('\n');
                    result.append("Состояние линии габаритных огней: "); result.append(responseParams.getFieldByKey("PLT").getValue().equals("0") ? "выключено (OFF)" : "включено (ON)"); result.append('\n');
                    result.append("Состояние линии ручника: "); result.append(responseParams.getFieldByKey("HB").getValue().equals("0") ? "выключено (OFF)" : "включено (ON)"); result.append('\n');
                    result.append("Состояние линии сигнала поворотников: "); result.append(responseParams.getFieldByKey("TS").getValue().equals("0") ? "выключено (OFF)" : "включено (ON)"); result.append('\n');
                    result.append("Время с RTC: "); result.append(responseParams.getFieldByKey("TIME").getValue()); result.append('\n');
                    result.append("Количество ошибок связи по I2C: "); result.append(responseParams.getFieldByKey("CER").getValue()); result.append('\n');
                    result.append("Количество накопленных оборотов коленчатого вала: "); result.append(responseParams.getFieldByKey("MC").getValue()); result.append('\n');
                    result.append("Количество мото-часов: "); result.append(responseParams.getFieldByKey("MHR").getValue()); result.append('\n');
                    result.append("Текущая надпись в строке статуса: "); result.append(new String(responseParams.getFieldByKey("SBC").getValue().getBytes("Cp1251"), "UTF-8")); result.append('\n');
                    result.append("Уровень топлива: "); result.append(responseParams.getFieldByKey("FL").getValue()); result.append(" л.\n");
                    result.append("Температура двигателя: "); result.append(responseParams.getFieldByKey("TP").getValue()); result.append(" °C");
                } catch (NotFoundException e) {
                    return "Ошибка во время обработки статистики.";
                } catch (UnsupportedEncodingException ignored) {
                }
                return "Текущая статистика:\n" + result;
            default:
                return "Неизвестный ответ";
        }
    }
}
