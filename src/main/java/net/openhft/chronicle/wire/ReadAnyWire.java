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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.CommonMarshallable;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.values.BooleanValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Represents a wire type that can be either {@code TextWire} or {@code BinaryWire}.
 * The specific wire type is determined dynamically based on the provided bytes.
 * This class provides flexibility in reading from wires that could be in either format.
 */
public class ReadAnyWire extends AbstractAnyWire implements Wire {

    /**
     * Constructs a new instance of {@code ReadAnyWire} with the provided bytes.
     * The specific wire type will be determined based on these bytes.
     *
     * @param bytes The bytes from which to determine the wire type.
     */
    public ReadAnyWire(@NotNull Bytes<?> bytes) {
        super(bytes, new ReadAnyWireAcquisition(bytes));
    }

    @Override
    public boolean isBinary() {
        return false; // as we don't know
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

    @Override
    public void reset() {
        clear();
    }

    @NotNull
    @Override
    public BooleanValue newBooleanReference() {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public boolean useSelfDescribingMessage(@NotNull CommonMarshallable object) {
        return object.usesSelfDescribingMessage();
    }

    @NotNull
    @Override
    public Bytes<?> bytes() {
        checkWire();
        return bytes;
    }

    /**
     * Represents a mechanism to acquire the correct wire type based on provided bytes.
     * This class is responsible for dynamically determining whether the bytes
     * correspond to a {@code TextWire} or a {@code BinaryWire}.
     */
    static class ReadAnyWireAcquisition implements WireAcquisition {
        private final Bytes<?> bytes;
        WireType wireType;
        @Nullable
        Wire wire = null;
        private ClassLookup classLookup = ClassAliasPool.CLASS_ALIASES;

        /**
         * Constructs a new instance of {@code ReadAnyWireAcquisition} with the provided bytes.
         *
         * @param bytes The bytes used to determine and acquire the appropriate wire type.
         */
        public ReadAnyWireAcquisition(Bytes<?> bytes) {
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
                    wireType = WireType.TEXT;
                } else if (BinaryWireCode.isFieldCode(bytes.readByte(bytes.readPosition()))) {
                    wireType = WireType.FIELDLESS_BINARY;
                } else {
                    wireType = WireType.BINARY;
                }

                final Wire wire = wireType.apply(bytes);
                wire.classLookup(classLookup);
                this.wire = wire;
                return wire;
            }

            return null;
        }

        public Bytes<?> bytes() {
            return bytes;
        }
    }
}
