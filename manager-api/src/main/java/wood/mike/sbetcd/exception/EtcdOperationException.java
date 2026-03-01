package wood.mike.sbetcd.exception;

public class EtcdOperationException extends RuntimeException {
    public EtcdOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
