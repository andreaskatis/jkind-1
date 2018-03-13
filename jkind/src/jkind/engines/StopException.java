package jkind.engines;

public class StopException extends RuntimeException {
	private static final long serialVersionUID = 1L;

    public StopException() {
    }

    public StopException(String message) {
        super(message);
    }

    public StopException(String message, Throwable cause) {
        super(message, cause);
    }

    public StopException(Throwable cause) {
        super(cause);
    }

    public StopException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
