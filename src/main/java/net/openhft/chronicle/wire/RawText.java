/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    /** The raw text content, stored as a {@link String}. */
    String text;

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
