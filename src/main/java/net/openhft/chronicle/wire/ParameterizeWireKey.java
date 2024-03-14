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

import org.jetbrains.annotations.NotNull;

/**
 * Represents a specialized {@code WireKey} that expects parameters to follow in a marshallable format.
 * Implementations of this interface are designed to handle parameterized wire keys, allowing for more
 * complex data representations on the wire.
 */
public interface ParameterizeWireKey extends WireKey {

    /**
     * Retrieves the parameters associated with this wire key. These parameters are expected
     * to be of type {@code WireKey} or its subtypes, ensuring compatibility with the wire format.
     *
     * @param <P> The type of the parameter, which extends {@code WireKey}.
     * @return An array of parameters associated with this wire key.
     */
    @NotNull <P extends WireKey> P[] params();
}
