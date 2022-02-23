/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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

import net.openhft.chronicle.core.annotation.DontChain;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * The defines the stand interface for writing and reading sequentially to/from a Bytes stream. <p>
 */
@DontChain
public interface Wire extends WireIn, WireOut {
    @Deprecated(/*to be removed?*/)
    static Wire fromFile(@NotNull String name) throws IOException {
        @NotNull String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        switch (ext) {
            case "csv":
                return CSVWire.fromFile(name);
            case "yaml":
                return TextWire.fromFile(name);
            default:
                throw new IllegalArgumentException("Unknown file type " + name);
        }
    }

    @Override
    @NotNull
    Wire headerNumber(long headerNumber);
}
