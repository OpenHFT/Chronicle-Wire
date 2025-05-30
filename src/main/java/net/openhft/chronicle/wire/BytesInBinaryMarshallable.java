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
 * Abstract base for marshallables that read and write directly to
 * {@link net.openhft.chronicle.bytes.Bytes} using a fixed binary layout.  These
 * objects do not include type information when serialized.
 */
public abstract class BytesInBinaryMarshallable extends AbstractCommonMarshallable {

    /**
     * Returns {@code false} as binary marshallables omit type information by default.
     */
    @Override
    public boolean usesSelfDescribingMessage() {
        return false;
    }
}
