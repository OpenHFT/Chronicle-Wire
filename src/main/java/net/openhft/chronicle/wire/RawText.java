/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
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
 * Represents a container for raw textual data.
 * The primary purpose of this class is to encapsulate a text
 * while providing a simple structure for raw text handling.
 *
 * @since 2023-09-11
 */
class RawText {
    // The encapsulated raw textual data
    String text;

    /**
     * Constructs a new instance of {@code RawText} initialized with
     * the provided CharSequence.
     *
     * @param text The CharSequence to be encapsulated by this instance.
     */
    public RawText(CharSequence text) {
        this.text = text.toString();
    }
}
