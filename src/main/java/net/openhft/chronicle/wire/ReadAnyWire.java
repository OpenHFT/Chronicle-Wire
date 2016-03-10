package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.ClassLookup;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * A wire type than can be either
 *
 * TextWire BinaryWire
 *
 * @author Rob Austin.
 */
public class ReadAnyWire extends AbstractAnyWire implements Wire {

    public ReadAnyWire(Bytes bytes) {
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
    public Bytes<?> bytes() {
        checkWire();
        return bytes;
    }

    static class ReadAnyWireAcquisition implements WireAcquisition {
        private final Bytes bytes;
        WireType wireType;
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

        @Override
        public Supplier<WireType> underlyingType() {
            return () -> wireType;
        }

        public Wire acquireWire() {
            if (wire != null)
                return wire;
            if (bytes.readRemaining() > 0) {
                int firstByte = bytes.readByte(0);

                if ((firstByte & 0x80) == 0) {
                    System.out.println("TEXT_WIRE");
                    wireType = WireType.TEXT;
                } else if (BinaryWireCode.isFieldCode(firstByte)) {
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
