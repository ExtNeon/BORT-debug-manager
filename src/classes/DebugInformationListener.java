package classes;

/**
 * Интерфейс - обработчик события. Единственный его метод вызывается в случае, если модуль прислал дебаг - информацию.
 */
public interface DebugInformationListener {
    void debugInformationGetted(BORT_response response);
}
