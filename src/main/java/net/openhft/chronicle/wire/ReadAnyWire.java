package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;

import static net.openhft.chronicle.wire.WireType.*;

/**
 * A wire type than can be either
 *
 * TextWire BinaryWire
 *
 * @author Rob Austin.
 */
public class ReadAnyWire implements Wire, InternalWire {

    private static class WireAcquisition {
        private final Bytes bytes;

        public WireAcquisition(Bytes bytes) {
            this.bytes = bytes;
        }

        InternalWire wire = null;

        private InternalWire acquireWire() {
            if (wire != null)
                return wire;
            if (bytes.readRemaining() > 0) {
                int code = bytes.readByte(0);

                if (code >= ' ' && code < 127)
                    return wire = (InternalWire) TEXT.apply(bytes);
                else if (BinaryWireCode.isFieldCode(code))
                    return wire = (InternalWire) FIELDLESS_BINARY.apply(bytes);

                return wire = (InternalWire) BINARY.apply(bytes);
            }

            throw new IllegalStateException("unknown wire type");

        }

        public Bytes bytes() {
            return bytes;
        }
    }

    /**
     * @return the type of wire for example Text or Binary
     */
    public Class<Wire> underlyingType() {
        return (Class<Wire>) ((Wire) wa.acquireWire()).getClass();
    }

    private WireAcquisition wa;

    public ReadAnyWire(Bytes bytes) {
        wa = new WireAcquisition(bytes);
    }


    @Override
    public void setReady(boolean ready) {
        wa.acquireWire().setReady(ready);
    }

    @Override
    public boolean isReady() {
        return wa.acquireWire().isReady();
    }

    @Override
    public void copyTo(@NotNull WireOut wire) {
        wa.acquireWire().copyTo(wire);
    }

    @NotNull
    @Override
    public ValueIn read() {
        return wa.acquireWire().read();
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull WireKey key) {
        return wa.acquireWire().read(key);
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull StringBuilder name) {
        return wa.acquireWire().read(name);
    }

    @NotNull
    @Override
    public ValueIn getValueIn() {
        return wa.acquireWire().getValueIn();
    }

    @NotNull
    @Override
    public Wire readComment(@NotNull StringBuilder sb) {
        return wa.acquireWire().readComment(sb);
    }

    @NotNull
    @Override
    public Bytes<?> bytes() {
        wa.acquireWire();
        return wa.bytes();
    }

    @NotNull
    @Override
    public IntValue newIntReference() {
        return wa.acquireWire().newIntReference();
    }

    @NotNull
    @Override
    public LongValue newLongReference() {
        return wa.acquireWire().newLongReference();
    }

    @NotNull
    @Override
    public LongArrayValues newLongArrayReference() {
        return wa.acquireWire().newLongArrayReference();
    }

    @Override
    public void clear() {
        wa.acquireWire();
        wa.bytes().clear();
    }

    @Override
    public boolean hasMore() {
        return wa.acquireWire().hasMore();
    }

    @Override
    public DocumentContext readingDocument() {
        return wa.acquireWire().readingDocument();
    }

    @Override
    public DocumentContext readingDocument(long readLocation) {
        return wa.acquireWire().readingDocument(readLocation);
    }

    @Override
    public void consumePadding() {
        wa.acquireWire().consumePadding();
    }

    @NotNull
    @Override
    public ValueOut write() {
        return wa.acquireWire().write();
    }

    @NotNull
    @Override
    public ValueOut write(WireKey key) {
        return wa.acquireWire().write(key);
    }

    @NotNull
    @Override
    public ValueOut getValueOut() {
        return wa.acquireWire().getValueOut();
    }

    @NotNull
    @Override
    public WireOut writeComment(CharSequence s) {
        return wa.acquireWire().writeComment(s);
    }

    @NotNull
    @Override
    public WireOut addPadding(int paddingToAdd) {
        return wa.acquireWire().addPadding(paddingToAdd);
    }

    @Override
    public DocumentContext writingDocument(boolean metaData) {
        return wa.acquireWire().writingDocument(metaData);
    }
}
