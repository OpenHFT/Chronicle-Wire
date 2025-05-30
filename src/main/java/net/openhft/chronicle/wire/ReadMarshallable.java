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

import net.openhft.chronicle.bytes.CommonMarshallable;
import net.openhft.chronicle.core.annotation.DontChain;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.jetbrains.annotations.NotNull;

/**
 * Contract for objects that reuse a pre-existing instance when reading from a
 * wire.  It avoids allocation in performance critical code.  For immutable
 * objects use {@link Demarshallable} instead.
 */
@FunctionalInterface
@DontChain
public interface ReadMarshallable extends CommonMarshallable {

    /** No-op implementation that simply skips the input. */
    ReadMarshallable DISCARD = w -> {};

    /**
     * Update this object by reading its fields from {@code wire}.
     */
    void readMarshallable(@NotNull WireIn wire) throws IORuntimeException, InvalidMarshallableException;

    /**
     * Invoked when a field is encountered that this object does not expect.
     * The default implementation skips the value.
     */
    default void unexpectedField(Object event, ValueIn valueIn) throws InvalidMarshallableException {
        valueIn.skipValue();
    }
}
