/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.values.BooleanValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A wire type than can be either
 * <p>
 * TextWire BinaryWire
 *
 * @author Rob Austin.
 */
public class ReadAnyWire extends AbstractAnyWire implements Wire {

    public ReadAnyWire(@NotNull Bytes bytes) {
        super(bytes, new ReadAnyWireAcquisition(bytes));
    }

    @Override
    public void classLookup(ClassLookup classLookup) {
        this.wireAcquisition.classLookup(classLookup);
    }

    @Override
    public ClassLookup classLookup() {
        return wireAcquisition.classLookup();
    }

    @Override
    public void clear() {
        checkWire();
        bytes.clear();
    }

    @NotNull
    @Override
    public BooleanValue newBooleanReference() {
        throw new UnsupportedOperationException("todo");
    }

    @NotNull
    @Override
    public Bytes<?> bytes() {
        checkWire();
        return bytes;
    }

    static class ReadAnyWireAcquisition implements WireAcquisition {
        private final Bytes bytes;
        WireType wireType;
        @Nullable
        Wire wire = null;
        private ClassLookup classLookup = ClassAliasPool.CLASS_ALIASES;

        public ReadAnyWireAcquisition(Bytes bytes) {
            this.bytes = bytes;
        }

        @Override
        public void classLookup(ClassLookup classLookup) {
            this.classLookup = classLookup;
            if (wire != null)
                wire.classLookup(classLookup);
        }

        @Override
        public ClassLookup classLookup() {
            return classLookup;
        }

        @NotNull
        @Override
        public Supplier<WireType> underlyingType() {
            return () -> wireType;
        }

        @Override
        @Nullable
        public Wire acquireWire() {
            if (wire != null)
                return wire;
            if (bytes.readRemaining() >= 8) {
                int firstBytes = bytes.readInt(bytes.readPosition()) |
                        bytes.readInt(bytes.readPosition() + 4);
                firstBytes |= firstBytes >> 16;
                firstBytes |= firstBytes >> 8;

                if ((firstBytes & 0x80) == 0) {
                    System.out.println("TEXT_WIRE");
                    wireType = WireType.TEXT;
                } else if (BinaryWireCode.isFieldCode(bytes.readByte(bytes.readPosition()))) {
                    System.out.println("FIELDLESS_BINARY");
                    wireType = WireType.FIELDLESS_BINARY;
                } else {
                    System.out.println("BINARY");
                    wireType = WireType.BINARY;
                }

                Wire wire = wireType.apply(bytes);
                wire.classLookup(classLookup);
                return this.wire = wire;
            }

            return null;
        }

        public Bytes bytes() {
            return bytes;
        }
    }
}
