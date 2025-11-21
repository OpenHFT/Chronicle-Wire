/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

/**
 * Represents a container for raw textual data. This class can be used as a
 * method argument type in method writers (see {@link VanillaMethodWriterBuilder})
 * to indicate that the provided {@link CharSequence} should be written to the
 * wire with minimal or no escaping, if supported by the wire type (for example,
 * {@link ValueOut#rawText(CharSequence)}). This is a package-private class,
 * intended for internal use within the Chronicle Wire framework.
 */
class RawText {
    // The encapsulated raw textual data
    final String text;

    /**
     * Constructs a new instance of {@code RawText} initialised with the provided
     * {@link CharSequence}.
     *
     * @param text The {@link CharSequence} whose content will be stored. It is
     *             converted to a {@link String} internally.
     */
    public RawText(CharSequence text) {
        this.text = text.toString();
    }
}
