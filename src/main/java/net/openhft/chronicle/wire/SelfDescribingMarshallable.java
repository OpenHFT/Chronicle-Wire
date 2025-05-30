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
 * Base class for marshallables that include their type information when written.
 * <p>
 * A self-describing marshallable writes its class name (and, for text wires,
 * the field names) into the stream so that a reader can reconstruct the object
 * without knowing its exact type.
 */
public abstract class SelfDescribingMarshallable extends AbstractCommonMarshallable {

    /**
     * Always returns {@code true} as these marshallables emit type metadata.
     */
    @Override
    public boolean usesSelfDescribingMessage() {
        return true;
    }
}
