//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire.method;

/**
 * Enum representing different service types.
 * Each enum constant represents a distinct service.
 */
public enum Service {

    // Enum constants representing different services
    S1, // Represents service type S1
    S2; // Represents service type S2

    /**
     * Returns the service ID as a string.
     * This method converts the enum constant to a string representation.
     *
     * @return The string representation of the enum constant, effectively the service ID.
     */
    String serviceId() {
        return toString(); // Returns the string representation of the enum constant
    }
}
