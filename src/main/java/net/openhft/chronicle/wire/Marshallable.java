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

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import static net.openhft.chronicle.wire.WireType.READ_ANY;
import static net.openhft.chronicle.wire.WireType.TEXT;

/**
 * The implementation of this interface is both readable and write-able as marshallable data.
 */
public interface Marshallable extends WriteMarshallable, ReadMarshallable {
    static boolean $equals(WriteMarshallable $this, Object o) {
        return o instanceof WriteMarshallable &&
                ($this == o ||
                        TEXT.asString($this).equals(TEXT.asString((WriteMarshallable) o)));
    }

    static int $hashCode(WriteMarshallable $this) {
        return HashWire.hash32($this);
    }

    static String $toString(WriteMarshallable $this) {
        return TEXT.asString($this);
    }

    static <T> T fromString(CharSequence cs) {
        return TEXT.fromString(cs);
    }

    static <T> T fromFile(String filename) throws IOException {
        return TEXT.fromFile(filename);
    }

    static Map<String, Object> fromFileAsMap(String filename) throws IOException {
        return TEXT.fromFileAsMap(filename, Object.class);
    }

    static <V> Map<String, V> fromFileAsMap(String filename, Class<V> valueClass) throws IOException {
        return TEXT.fromFileAsMap(filename, valueClass);
    }

    static Map<String, Object> fromHexString(CharSequence cs) throws IOException {
        return READ_ANY.fromHexString(cs);
    }

    @Override
    default void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        Wires.readMarshallable(this, wire);
    }

    @Override
    default void writeMarshallable(@NotNull WireOut wire) {
        Wires.writeMarshallable(this, wire);
    }

    default <T extends Marshallable> T deepCopy() {
        return (T) Wires.deepCopy(this);
    }

    default <T extends Marshallable> T copyFrom(T t) {
        return Wires.copyFrom(t, this);
    }

    default <K, T extends Marshallable> T mergeToMap(Map<K, T> map, Function<T, K> getKey) {
        @SuppressWarnings("unchecked")
        T t = (T) this;
        return map.merge(getKey.apply(t), t,
                (p, c) -> p == null ? c.deepCopy() : p.copyFrom(c));
    }
}
