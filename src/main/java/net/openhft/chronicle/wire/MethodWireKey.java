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
 * The {@code MethodWireKey} class represents a key within a wire format, extending the
 * {@link BytesInBinaryMarshallable} class and implementing the {@link WireKey} interface.
 * Each instance of {@code MethodWireKey} has a unique name and code combination. The name,
 * if not provided, defaults to the string representation of the code.
 * <p>
 * This class can be particularly useful in scenarios where wire keys are required to be identified
 * both by a textual name and a numeric code.
 * </p>
 */
public class MethodWireKey extends BytesInBinaryMarshallable implements WireKey {

    // The name of the wire key
    private final String name;

    // The numeric code representing the wire key
    private final int code;

    /**
     * Constructs a new {@code MethodWireKey} with the provided name and code.
     *
     * @param name The name of the wire key.
     * @param code The numeric code representing the wire key.
     */
    public MethodWireKey(String name, int code) {
        this.name = name;
        this.code = code;
    }

    @NotNull
    @Override
    public String name() {
        return name == null ? Integer.toString(code) : name;
    }

    @Override
    public int code() {
        return code;
    }
}
