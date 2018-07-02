package iniSettings;

import iniSettings.exceptions.RecordParsingException;

/**
 * Класс представляет запись формата key = value, использующуюся в формате INI
 * Суть - контейнер с двумя полями. Позволяет менять значения key и value.
 *
 * @author Малякин Кирилл.  15ИТ20.
 */
public class INISettingsRecord {
    private String key, value;

    public INISettingsRecord(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public INISettingsRecord(String string) throws RecordParsingException {
        if (!string.contains("=")) {
            throw new RecordParsingException();
        }
        string = string.trim();
        key = string.substring(0, string.indexOf('='));
        try {
            value = string.substring(string.indexOf('=') + 1, string.length());
        } catch (IndexOutOfBoundsException e) {
            value = "";
        }
    }

    public String getKey() {
        return key;
    }


    public String getValue() {
        return value;
    }


    @Override
    public String toString() {
        return key + '=' + value;
    }
}
