//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

/**
 * Exception thrown when a generated {@link MethodWriter} cannot be created.
 *
 * <p>Generated writers are typically faster and richer than reflection based
 * proxies. When the generator configuration fails validation this exception is
 * thrown so the system does not fall back to the proxy implementation.
 *
 * @see GenerateMethodWriter
 * @see VanillaMethodWriterBuilder
 */
public class MethodWriterValidationException extends IllegalArgumentException {
    private static final long serialVersionUID = 0L;

    /**
     * Constructs a new {@code MethodWriterValidationException} with the specified detail message.
     *
     * @param s the detail message explaining the validation failure
     */
    public MethodWriterValidationException(String s) {
        super(s);
    }
}
