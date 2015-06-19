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
import net.openhft.chronicle.bytes.BytesMarshaller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public class MarshallableBytesMarshaller<M extends Marshallable> implements BytesMarshaller<M> {
    private final Function<Bytes, Wire> wireFactory;
    private final Supplier<M> mSupplier;

    private MarshallableBytesMarshaller(Function<Bytes, Wire> wireFactory, Supplier<M> mSupplier) {
        this.wireFactory = wireFactory;
        this.mSupplier = mSupplier;
    }

    @NotNull
    public static <M extends Marshallable> MarshallableBytesMarshaller<M> of(Function<Bytes, Wire> wireFactory, Supplier<M> mSupplier) {
        return new MarshallableBytesMarshaller<>(wireFactory, mSupplier);
    }

    @Override
    public void write(Bytes bytes, @NotNull M m) {
        m.writeMarshallable(wireFactory.apply(bytes));
    }

    @Nullable
    @Override
    public M read(Bytes bytes, @Nullable M m) {
        if (m == null)
            m = mSupplier.get();
        m.readMarshallable(wireFactory.apply(bytes));
        return m;
    }
}
