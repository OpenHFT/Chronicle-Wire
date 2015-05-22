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

package net.openhft.chronicle.wire.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

import static java.lang.ThreadLocal.withInitial;

public class WireUtil {

    public static ThreadLocal<ByteableLongArrayValues> newLongArrayValuesPool(
            @NotNull Class<? extends Wire> wireType) {
        if (TextWire.class.isAssignableFrom(wireType)) {
            return withInitial(TextLongArrayReference::new);

        } else if (BinaryWire.class.isAssignableFrom(wireType)) {
            return withInitial(BinaryLongArrayReference::new);

        } else {
            throw new IllegalStateException("todo, unsupported type=" + wireType);
        }
    }

    public static Function<Bytes, Wire> byteToWireFor(
            @NotNull Class<? extends Wire> wireType) {
        if (TextWire.class.isAssignableFrom(wireType)) {
            return TextWire::new;

        } else if (BinaryWire.class.isAssignableFrom(wireType)) {
            return BinaryWire::new;

        } else if (RawWire.class.isAssignableFrom(wireType)) {
            return RawWire::new;

        } else {
            throw new UnsupportedOperationException("todo (byteToWireFor)");
        }
    }

    public static Wire createWire(
            @NotNull final Class<? extends Wire> wireType,
            @NotNull final Bytes bytes) {
        return byteToWireFor(wireType).apply(bytes);
    }
}
