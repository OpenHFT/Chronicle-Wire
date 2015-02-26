package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesMarshaller;

import java.util.function.Function;
import java.util.function.Supplier;

public class MarshallableBytesMarshaller<M extends Marshallable> implements BytesMarshaller<M> {
    private final Function<Bytes, Wire> wireFactory;
    private final Supplier<M> mSupplier;

    private MarshallableBytesMarshaller(Function<Bytes, Wire> wireFactory, Supplier<M> mSupplier) {
        this.wireFactory = wireFactory;
        this.mSupplier = mSupplier;
    }

    public static <M extends Marshallable> MarshallableBytesMarshaller<M> of(Function<Bytes, Wire> wireFactory, Supplier<M> mSupplier) {
        return new MarshallableBytesMarshaller<>(wireFactory, mSupplier);
    }

    @Override
    public void write(Bytes bytes, M m) {
        m.writeMarshallable(wireFactory.apply(bytes));
    }

    @Override
    public M read(Bytes bytes, M m) {
        if (m == null)
            m = mSupplier.get();
        m.readMarshallable(wireFactory.apply(bytes));
        return m;
    }
}
