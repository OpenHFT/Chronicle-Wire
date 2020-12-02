package net.openhft.chronicle.wire;

/**
 * Thrown if we cannot generate a {@link MethodWriter} and should not fall back to proxy impl.
 */
public class MethodWriterValidationException extends IllegalArgumentException {
    public MethodWriterValidationException(String s) {
        super(s);
    }
}
