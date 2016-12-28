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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Used typically for writing in conjunction with a readAny wire
 *
 * Ideal when some bytes have to be read before the type is know, this type is subsequently set via
 * {@code wireTypeSupplier}
 *
 * TextWire BinaryWire
 *
 * @author Rob Austin.
 */
public class DeferredTypeWire extends AbstractAnyWire implements Wire {

    public DeferredTypeWire(@NotNull Bytes bytes, Supplier<WireType> wireTypeSupplier) {
        super(bytes, new DeferredTypeWireAcquisition(bytes, wireTypeSupplier));
    }

    @Override
    public void classLookup(ClassLookup classLookup) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassLookup classLookup() {
        return ClassAliasPool.CLASS_ALIASES;
    }

    @Override
    public void clear() {
        checkWire();
        bytes.clear();
    }

    @NotNull
    @Override
    public Bytes<?> bytes() {
        checkWire();
        return bytes;
    }

    static class DeferredTypeWireAcquisition implements WireAcquisition {
        private final Bytes bytes;
        private final Supplier<WireType> wireTypeSupplier;
        @Nullable
        private Wire wire = null;
        private WireType wireType;

        public DeferredTypeWireAcquisition(Bytes bytes, Supplier<WireType> wireTypeSupplier) {
            this.bytes = bytes;
            this.wireTypeSupplier = wireTypeSupplier;
        }

        @NotNull
        @Override
        public Supplier<WireType> underlyingType() {
            return () -> wireType;
        }

        @Nullable
        public Wire acquireWire() {
            if (wire != null)
                return wire;
            wireType = wireTypeSupplier.get();
            if (wireType == null)
                throw new IllegalStateException("unknown type");
            return wireType.apply(bytes);

        }

        @Override
        public void classLookup(ClassLookup classLookup) {

        }

        @Nullable
        @Override
        public ClassLookup classLookup() {
            return null;
        }

        public Bytes bytes() {
            return bytes;
        }
    }
}
