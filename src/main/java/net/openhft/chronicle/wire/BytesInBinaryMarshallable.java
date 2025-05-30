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
 * Abstract base for marshallables that handle their own binary layout.
 * <p>
 * These objects are typically written and read as a block of bytes rather than
 * through field-by-field wire operations. They do not embed type information
 * when serialized via the standard wire mechanisms.
 */
public abstract class BytesInBinaryMarshallable extends AbstractCommonMarshallable {

    /**
     * Returns {@code false} as binary marshallables omit explicit type metadata.
     */
    @Override
    public boolean usesSelfDescribingMessage() {
        return false;
    }
}
