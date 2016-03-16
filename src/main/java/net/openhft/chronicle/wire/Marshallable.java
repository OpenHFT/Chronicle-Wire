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
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;

import static net.openhft.chronicle.wire.WireType.TEXT;

/**
 * The implementation of this interface is both readable and write-able as marshallable data.
 */
public interface Marshallable extends WriteMarshallable, ReadMarshallable {
    static boolean $equals(WriteMarshallable $this, Object o) {
        return o instanceof WriteMarshallable &&
                TEXT.asString($this).equals(TEXT.asString((WriteMarshallable) o));
    }

    static int $hashCode(WriteMarshallable $this) {
        return HashWire.hash32($this);
    }

    static String $toString(WriteMarshallable $this) {
        return TEXT.asString($this);
    }

    @Override
    default void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        Wires.readMarshallable(this, wire);
    }

    @Override
    default void writeMarshallable(@NotNull WireOut wire) {
        Wires.writeMarshallable(this, wire);
    }

}
