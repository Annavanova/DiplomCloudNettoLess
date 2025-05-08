package DiplomCloud.DiplomCloud.exception;

public class CriticalOperationException extends RuntimeException {
    public CriticalOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
