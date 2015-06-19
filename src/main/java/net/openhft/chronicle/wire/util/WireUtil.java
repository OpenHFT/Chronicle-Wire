/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
