package classes;

/**
 * Интерфейс - обработчик события. Его методы вызываются в случае, если модуль прислал дебаг - информацию.
 */
public interface DebugInformationListener {
    void debugInformationReceive(BORT_response response);

    void errorMessageNotifyReceive(String message);
}
