package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * A wire type than can be either
 *
 * TextWire BinaryWire
 *
 * @author Rob Austin.
 */
public abstract class AbstractAnyWire implements Wire, InternalWire {

    protected final WireAcquisition wireAcquisition;

    public AbstractAnyWire(@NotNull WireAcquisition wa) {
        this.wireAcquisition = wa;
    }

    public Wire underlyingWire() {
        return wireAcquisition.acquireWire();
    }

    public Supplier<WireType> underlyingType() {
        return wireAcquisition.underlyingType();
    }

    @Override
    public boolean isReady() {
        return wireAcquisition.acquireWire().isReady();
    }

    @Override
    public void setReady(boolean ready) {
        wireAcquisition.acquireWire().setReady(ready);
    }

    @Override
    public void copyTo(@NotNull WireOut wire) {
        wireAcquisition.acquireWire().copyTo(wire);
    }

    @NotNull
    @Override
    public ValueIn read() {
        return wireAcquisition.acquireWire().read();
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull WireKey key) {
        return wireAcquisition.acquireWire().read(key);
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull StringBuilder name) {
        return wireAcquisition.acquireWire().read(name);
    }

    @NotNull
    @Override
    public ValueIn getValueIn() {
        return wireAcquisition.acquireWire().getValueIn();
    }

    @NotNull
    @Override
    public Wire readComment(@NotNull StringBuilder sb) {
        return wireAcquisition.acquireWire().readComment(sb);
    }

    @NotNull
    @Override
    public IntValue newIntReference() {
        return wireAcquisition.acquireWire().newIntReference();
    }

    @NotNull
    @Override
    public LongValue newLongReference() {
        return wireAcquisition.acquireWire().newLongReference();
    }

    @NotNull
    @Override
    public LongArrayValues newLongArrayReference() {
        return wireAcquisition.acquireWire().newLongArrayReference();
    }

    void checkWire() {
        wireAcquisition.acquireWire();
    }

    @Override
    public boolean hasMore() {
        return wireAcquisition.acquireWire().hasMore();
    }

    @Override
    public DocumentContext readingDocument() {
        return wireAcquisition.acquireWire().readingDocument();
    }

    @Override
    public DocumentContext readingDocument(long readLocation) {
        return wireAcquisition.acquireWire().readingDocument(readLocation);
    }

    @Override
    public void consumePadding() {
        wireAcquisition.acquireWire().consumePadding();
    }

    @NotNull
    @Override
    public ValueOut write() {
        return wireAcquisition.acquireWire().write();
    }

    @NotNull
    @Override
    public ValueOut write(WireKey key) {
        return wireAcquisition.acquireWire().write(key);
    }

    @NotNull
    @Override
    public ValueOut getValueOut() {
        return wireAcquisition.acquireWire().getValueOut();
    }

    @NotNull
    @Override
    public WireOut writeComment(CharSequence s) {
        return wireAcquisition.acquireWire().writeComment(s);
    }

    @NotNull
    @Override
    public WireOut addPadding(int paddingToAdd) {
        return wireAcquisition.acquireWire().addPadding(paddingToAdd);
    }

    @Override
    public DocumentContext writingDocument(boolean metaData) {
        return wireAcquisition.acquireWire().writingDocument(metaData);
    }

    interface WireAcquisition {

        /**
         * @return the type of wire for example Text or Binary
         */
        Supplier<WireType> underlyingType();

        InternalWire acquireWire();

        void classLookup(ClassLookup classLookup);

        ClassLookup classLookup();
    }
}
