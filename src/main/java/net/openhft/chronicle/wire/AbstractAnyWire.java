/*
 *
 *  *     Copyright (C) 2016  higherfrequencytrading.com
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU Lesser General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU Lesser General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU Lesser General Public License
 *  *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
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
public abstract class AbstractAnyWire extends AbstractWire implements Wire {

    protected final WireAcquisition wireAcquisition;

    public AbstractAnyWire(Bytes bytes, @NotNull WireAcquisition wa) {
        super(bytes, false);
        this.wireAcquisition = wa;
    }

    public Wire underlyingWire() {
        return wireAcquisition.acquireWire();
    }

    public Supplier<WireType> underlyingType() {
        return wireAcquisition.underlyingType();
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

        Wire acquireWire();

        void classLookup(ClassLookup classLookup);

        ClassLookup classLookup();
    }
}
