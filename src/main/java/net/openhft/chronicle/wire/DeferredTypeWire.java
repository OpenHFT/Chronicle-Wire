package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
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
public class DeferredTypeWire extends AbstractAnyWire implements Wire, InternalWire {

    private final Bytes bytes;

    public DeferredTypeWire(Bytes bytes, Supplier<WireType> wireTypeSupplier) {
        super(new DeferredTypeWireAcquisition(bytes, wireTypeSupplier));
        this.bytes = bytes;
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

    private static class DeferredTypeWireAcquisition implements WireAcquisition {
        private final Bytes bytes;
        private final Supplier<WireType> wireTypeSupplier;
        private InternalWire wire = null;
        private WireType wireType;

        public DeferredTypeWireAcquisition(Bytes bytes, Supplier<WireType> wireTypeSupplier) {
            this.bytes = bytes;
            this.wireTypeSupplier = wireTypeSupplier;
        }

        @Override
        public Supplier<WireType> underlyingType() {
            return () -> wireType;
        }

        public InternalWire acquireWire() {
            if (wire != null)
                return wire;
            wireType = wireTypeSupplier.get();
            if (wireType == null)
                throw new IllegalStateException("unknown type");
            return (InternalWire) wireType.apply(bytes);

        }

        public Bytes bytes() {
            return bytes;
        }
    }

}
