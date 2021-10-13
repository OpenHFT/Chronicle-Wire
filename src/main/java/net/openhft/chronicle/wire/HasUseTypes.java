package net.openhft.chronicle.wire;

public interface HasUseTypes<T extends Wire> {

    /**
     * Sets if this Wire shall use explicit types both for generating and parsing wire messages.
     *
     * @param useTypes false - never use types, true - always use types
     * @return this
     */
    T useTypes(boolean useTypes);

    /**
     * Returns if this Wire shall use explicit types both for generating and parsing wire messages.
     *
     * @return if this Wire shall use explicit types both for generating and parsing wire messages
     */
    boolean useTypes();

}