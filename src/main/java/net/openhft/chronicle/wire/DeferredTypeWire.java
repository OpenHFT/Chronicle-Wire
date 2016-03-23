/*
 *
 *  *     Copyright (C) 2016  higherfrequencytrading.com
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU Lesser General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU Lesser General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU Lesser General Public License
 *  *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.ClassLookup;
import org.jetbrains.annotations.NotNull;

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

    public DeferredTypeWire(Bytes bytes, Supplier<WireType> wireTypeSupplier) {
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
        private Wire wire = null;
        private WireType wireType;

        public DeferredTypeWireAcquisition(Bytes bytes, Supplier<WireType> wireTypeSupplier) {
            this.bytes = bytes;
            this.wireTypeSupplier = wireTypeSupplier;
        }

        @Override
        public Supplier<WireType> underlyingType() {
            return () -> wireType;
        }

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

        @Override
        public ClassLookup classLookup() {
            return null;
        }

        public Bytes bytes() {
            return bytes;
        }
    }

}
