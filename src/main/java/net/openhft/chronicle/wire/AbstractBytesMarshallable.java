package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;

public class AbstractBytesMarshallable extends AbstractMarshallable {
    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        if (wire instanceof BinaryWire) {
            readMarshallable(((BinaryWire) wire).bytes);
        } else {
            super.readMarshallable(wire);
        }
    }

    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        if (wire instanceof BinaryWire) {
            writeMarshallable(((BinaryWire) wire).bytes);
        } else {
            super.writeMarshallable(wire);
        }
    }
}
