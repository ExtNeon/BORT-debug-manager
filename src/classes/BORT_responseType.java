package classes;

/**
 * Список всех возможных типов ответов от модуля
 */
public enum BORT_responseType {
    MODULE_STARTED,
    DEBUG_INFORMATION,
    ECHO,
    PARAM_RW_SUCCESS,
    EEPROM_RESET_SUCCESS,
    RESTART_CMD_GETTED,
    WRITE_CMD_GETTED,
    READ_CMD_GETTED,
    RTC_TIME_READ_SUCCESS,
    EEPROM_SAVE_CMD_GETTED,
    EEPROM_LOAD_CMD_GETTED,
    ERR_WRONG_PARAM_NAME,
    ERR_WRONG_CMD_TYPE,
    ERR_RTC_UNSETTED,
    ERR_RTC_CONNECTION_FAILED,
    INFO_STATISTICS,
    UNKNOWN;

    public static BORT_responseType getResponseTypeFromNumericalCode(int code) {
        switch (code) {
            case 101:
                return MODULE_STARTED;
            case 111:
                return DEBUG_INFORMATION;
            case 115:
                return ECHO;
            case 303:
                return PARAM_RW_SUCCESS;
            case 305:
                return EEPROM_RESET_SUCCESS;
            case 310:
                return RESTART_CMD_GETTED;
            case 320:
                return WRITE_CMD_GETTED;
            case 330:
                return READ_CMD_GETTED;
            case 340:
                return EEPROM_SAVE_CMD_GETTED;
            case 350:
                return EEPROM_LOAD_CMD_GETTED;
            case 360:
                return INFO_STATISTICS;
            case 371:
                return RTC_TIME_READ_SUCCESS;
            case 401:
                return ERR_WRONG_PARAM_NAME;
            case 407:
                return ERR_WRONG_CMD_TYPE;
            case 453:
                return ERR_RTC_UNSETTED;
            case 461:
                return ERR_RTC_CONNECTION_FAILED;
            default:
                return UNKNOWN;
        }
    }
}
