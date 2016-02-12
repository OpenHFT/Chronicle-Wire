package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * A wire type than can be either
 *
 * TextWire BinaryWire
 *
 * @author Rob Austin.
 */
public class ReadAnyWire extends AbstractAnyWire implements Wire, InternalWire {

    private final Bytes bytes;

    public ReadAnyWire(Bytes bytes) {
        super(new ReadAnyWireAcquisition(bytes));
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

    private static class ReadAnyWireAcquisition implements WireAcquisition {
        private final Bytes bytes;

        WireType wireType;

        public ReadAnyWireAcquisition(Bytes bytes) {
            this.bytes = bytes;
        }

        InternalWire wire = null;

        @Override
        public Supplier<WireType> underlyingType() {
            return () -> wireType;
        }

        public InternalWire acquireWire() {
            if (wire != null)
                return wire;
            if (bytes.readRemaining() > 0) {
                int firstByte = bytes.readByte(0);
                if (firstByte < ' ' && firstByte != '\n') {
                    System.out.println("TEXT_WIRE");
                    wireType = WireType.TEXT;
                } else if (BinaryWireCode.isFieldCode(firstByte)) {
                    System.out.println("FIELDLESS_BINARY");
                    wireType = WireType.FIELDLESS_BINARY;
                } else {
                    System.out.println("BINARY");
                    wireType = WireType.BINARY;
                }

                return wire = (InternalWire) wireType.apply(bytes);
            }

            return null;
        }

        public Bytes bytes() {
            return bytes;
        }

    }
}
