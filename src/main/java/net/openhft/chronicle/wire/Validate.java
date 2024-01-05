package net.openhft.chronicle.wire;

/**
 * This is the Validate interface.
 * Implementations of this interface are responsible for validating objects
 * based on specific criteria or conditions.
 * The purpose of the validate method is to ensure the correctness or suitability
 * of the object in question.
 */
public interface Validate {

    /**
     * Validates the provided object based on specific criteria or conditions.
     * If the object does not meet the conditions, the method might throw a
     * runtime exception or exhibit other behavior as defined by the
     * implementation. It is recommended for implementers to clearly document
     * the validation rules and potential outcomes.
     *
     * @param o The object to be validated.
     */
    void validate(Object o);
}
