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

import net.openhft.chronicle.bytes.Bytes;

import java.util.function.Function;

public enum WireType implements Function<Bytes, Wire> {
    TEXT {
        @Override
        public Wire apply(Bytes bytes) {
            return new TextWire(bytes);
        }
    }, BINARY {
        @Override
        public Wire apply(Bytes bytes) {
            return new BinaryWire(bytes);
        }
    }, FIELDLESS_BINARY {
        @Override
        public Wire apply(Bytes bytes) {
            return new BinaryWire(bytes, false, false, true);
        }
    }, RAW {
        @Override
        public Wire apply(Bytes bytes) {
            return new RawWire(bytes);
        }
    }
}