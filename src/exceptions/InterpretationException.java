package exceptions;

/**
 * Выбрасывается в случае проблем с обработкой ответа от модуля.
 */
public class InterpretationException extends Exception {
    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public InterpretationException(String message) {
        super(message);
    }
}
