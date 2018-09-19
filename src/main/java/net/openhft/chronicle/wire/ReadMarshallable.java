/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;

/**
 * This interface marks a object which can be reloaded from stream re-using an
 * existing object.
 * <p/>
 * For objects which must deserialize final field see Demarshallable
 */
@FunctionalInterface
public interface ReadMarshallable {
    ReadMarshallable DISCARD = w -> {
    };

    /**
     * Straight line ordered decoding.
     *
     * @param wire to read from in an ordered manner.
     * @throws IORuntimeException the stream wasn't ordered or formatted as expected.
     */
    void readMarshallable(@NotNull WireIn wire) throws IORuntimeException;

    default void unexpectedField(Object event, ValueIn valueIn) {
        valueIn.skipValue();
    }
}
