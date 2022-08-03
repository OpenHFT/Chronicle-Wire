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

public class MethodWireKey extends BytesInBinaryMarshallable implements WireKey {
    private final String name;
    private final int code;

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
