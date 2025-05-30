/*
 * Copyright 2016-2025 chronicle.software
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
 * A {@link Wire} that inspects the supplied bytes to decide whether they
 * represent text or binary wire format.  The underlying type is discovered only
 * on first access.  This class is primarily intended for reading from an
 * unknown source, although once the type is known it will delegate any write
 * operations to the resolved wire.
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

    /**
     * {@inheritDoc}
     *
     * <p>
     * Returns {@code false} until the initial bytes are inspected and the
     * underlying wire type is known.
     * </p>
     */
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
     * Implements the {@link WireAcquisition} strategy for {@link ReadAnyWire}.
     * It peeks at the initial bytes of the stream to decide whether the data is
     * text, binary or fieldless binary format.
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

        /**
         * Lazily determines and returns the resolved wire.
         * <p>
         * If the wire type has not been resolved, this method peeks at the
         * first eight bytes.  When all of those bytes have the top bit clear it
         * assumes {@link WireType#TEXT}.  If the first byte indicates a
         * {@link BinaryWireCode#FIELD_NUMBER} it uses
         * {@link WireType#FIELDLESS_BINARY}; otherwise {@link WireType#BINARY}
         * is chosen.  The resulting wire is cached for future calls.
         */
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

        /**
         * Returns the {@link Bytes} instance being inspected.
         */
        public Bytes<?> bytes() {
            return bytes;
        }
    }
}
