package net.openhft.chronicle.wire;

/**
 * Created by peter on 22/05/16.
 */
public class UnrecoverableTimeoutException extends IllegalStateException {
    public UnrecoverableTimeoutException(Exception e) {
        super(e.getMessage());
        initCause(e);
    }
}
