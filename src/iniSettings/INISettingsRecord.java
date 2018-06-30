package iniSettings;

import iniSettings.exceptions.RecordParsingException;

/**
 * Класс представляет запись формата key = value, использующуюся в формате INI
 * Суть - контейнер с двумя полями. Позволяет менять значения key и value.
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

// --Commented out by Inspection START (01.07.2018 0:18):
//    public void setKey(String key) {
//        this.key = key;
//    }
// --Commented out by Inspection STOP (01.07.2018 0:18)

    public String getValue() {
        return value;
    }

// --Commented out by Inspection START (01.07.2018 0:18):
//    public void setValue(String value) {
//        this.value = value;
//    }
// --Commented out by Inspection STOP (01.07.2018 0:18)

    @Override
    public String toString() {
        return key + '=' + value;
    }
}
