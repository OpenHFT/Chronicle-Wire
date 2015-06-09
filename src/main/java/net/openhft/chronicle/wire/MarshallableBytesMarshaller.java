/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
