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

        private InternalWire aquireWire() {
            if (wire != null)
                return wire;
            if (bytes.readRemaining() > 0) {
                int code = bytes.readByte(0);

                if (code >= ' ' && code < 127)
                    return wire = (InternalWire) TEXT.apply(bytes);
                else if (BinaryWireCode.isFieldCode(code))
                    return wire = (InternalWire) FIELDLESS_BINARY.apply(bytes);
                else
                    return wire = (InternalWire) BINARY.apply(bytes);
            }

            throw new IllegalStateException("unknown wire type");

        }
    }

    /**
     * @return the type of wire for example Text or Binary
     */
    public Class<Wire> underlyingType() {
        return (Class<Wire>) ((Wire) wa.aquireWire()).getClass();
    }

    private WireAcquisition wa;

    public ReadAnyWire(Bytes bytes) {
        wa = new WireAcquisition(bytes);
    }


    @Override
    public void setReady(boolean ready) {
        wa.aquireWire().setReady(ready);
    }

    @Override
    public boolean isReady() {
        return wa.aquireWire().isReady();
    }

    @Override
    public void copyTo(@NotNull WireOut wire) {
        wa.aquireWire().copyTo(wire);
    }

    @NotNull
    @Override
    public ValueIn read() {
        return wa.aquireWire().read();
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull WireKey key) {
        return wa.aquireWire().read(key);
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull StringBuilder name) {
        return wa.aquireWire().read(name);
    }

    @NotNull
    @Override
    public ValueIn getValueIn() {
        return wa.aquireWire().getValueIn();
    }

    @NotNull
    @Override
    public Wire readComment(@NotNull StringBuilder sb) {
        return wa.aquireWire().readComment(sb);
    }

    @NotNull
    @Override
    public Bytes<?> bytes() {
        return wa.aquireWire().bytes();
    }

    @NotNull
    @Override
    public IntValue newIntReference() {
        return wa.aquireWire().newIntReference();
    }

    @NotNull
    @Override
    public LongValue newLongReference() {
        return wa.aquireWire().newLongReference();
    }

    @NotNull
    @Override
    public LongArrayValues newLongArrayReference() {
        return wa.aquireWire().newLongArrayReference();
    }

    @Override
    public void clear() {
        wa.aquireWire().clear();
    }

    @Override
    public boolean hasMore() {
        return wa.aquireWire().hasMore();
    }

    @Override
    public DocumentContext readingDocument() {
        return wa.aquireWire().readingDocument();
    }

    @Override
    public DocumentContext readingDocument(long readLocation) {
        return wa.aquireWire().readingDocument(readLocation);
    }

    @Override
    public void consumePadding() {
        wa.aquireWire().consumePadding();
    }

    @NotNull
    @Override
    public ValueOut write() {
        return wa.aquireWire().write();
    }

    @NotNull
    @Override
    public ValueOut write(WireKey key) {
        return wa.aquireWire().write(key);
    }

    @NotNull
    @Override
    public ValueOut getValueOut() {
        return wa.aquireWire().getValueOut();
    }

    @NotNull
    @Override
    public WireOut writeComment(CharSequence s) {
        return wa.aquireWire().writeComment(s);
    }

    @NotNull
    @Override
    public WireOut addPadding(int paddingToAdd) {
        return wa.aquireWire().addPadding(paddingToAdd);
    }

    @Override
    public DocumentContext writingDocument(boolean metaData) {
        return wa.aquireWire().writingDocument(metaData);
    }
}
