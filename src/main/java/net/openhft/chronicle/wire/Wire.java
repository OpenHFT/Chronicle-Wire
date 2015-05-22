/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * The defines the stand interface for writing and reading sequentially to/from a Bytes stream.
 *
 * Created by peter.lawrey on 12/01/15.
 */
public interface Wire extends WireIn, WireOut {

    static Function<Bytes, Wire> bytesToWire(@NotNull Class<? extends Wire> wireClass) {
        if (TextWire.class.isAssignableFrom(wireClass))
            return TextWire::new;
        else if (BinaryWire.class.isAssignableFrom(wireClass))
            return BinaryWire::new;
        else
            throw new UnsupportedOperationException("type " + wireClass.getSimpleName() + " is not currently supported.");
    }
}
