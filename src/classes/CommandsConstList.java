package classes;

/**
 * Просто список всех констант с командами, которые я вынес в отдельный класс.
 */
public final class CommandsConstList {
    public static final String CMD_READ_TIME_FROM_RTC = "$8:";
    public static final String CMD_SET = "$2:";
    public static final String CMD_RESTART_MODULE = "$1:";
    public static final String CMD_READ_PARAM = "$3:";
    public static final String CMD_SAVE_SETTINGS_TO_EEPROM = "$4:";
    public static final String CMD_REQUEST_STATISTICS = "$7:";
    static final String CMD_CONNECTION_APPROVED = "$99:";
}
