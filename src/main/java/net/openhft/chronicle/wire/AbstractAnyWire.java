/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.values.IntArrayValues;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Base class for wires that determine whether they are text or binary on demand.
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractAnyWire extends AbstractWire implements Wire {

    /**
     * Strategy for lazily acquiring the concrete wire implementation.
     */
    @NotNull
    protected final WireAcquisition wireAcquisition;

    /**
     * Constructs a new instance of {@code AbstractAnyWire} using the specified bytes and wire acquisition strategy.
     *
     * @param bytes The byte storage and manipulation object.
     * @param wa    The strategy to acquire the actual wire type.
     */
    protected AbstractAnyWire(@NotNull Bytes<?> bytes, @NotNull WireAcquisition wa) {
        super(bytes, false);
        this.wireAcquisition = wa;
    }

    /**
     * Returns the lazily acquired underlying wire, creating it if necessary.
     */
    @Nullable
    public Wire underlyingWire() {
        return wireAcquisition.acquireWire();
    }

    /**
     * Supplies the {@link WireType} of the underlying wire.
     */
    @NotNull
    public Supplier<WireType> underlyingType() {
        return wireAcquisition.underlyingType();
    }

    @Override
    public void copyTo(@NotNull WireOut wire) throws InvalidMarshallableException {
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

    @Nullable
    @Override
    public <K> K readEvent(Class<K> expectedClass) throws InvalidMarshallableException {
        return wireAcquisition.acquireWire().readEvent(expectedClass);
    }

    @Override
    public void writeStartEvent() {
        wireAcquisition.acquireWire().writeStartEvent();
    }

    @Override
    public void writeEndEvent() {
        wireAcquisition.acquireWire().writeEndEvent();
    }

    @NotNull
    @Override
    public ValueIn getValueIn() {
        return wireAcquisition.acquireWire().getValueIn();
    }

    @NotNull
    @Override
    public WireIn readComment(@NotNull StringBuilder sb) {
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

    @Override
    public @NotNull IntArrayValues newIntArrayReference() {
        return wireAcquisition.acquireWire().newIntArrayReference();
    }

    /**
     * Ensures the underlying wire has been created.
     */
    void checkWire() {
        wireAcquisition.acquireWire();
    }

    @NotNull
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
        final Wire wire = wireAcquisition.acquireWire();
        wire.commentListener(commentListener);
        wire.consumePadding();
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

    @Override
    public ValueOut write(CharSequence key) {
        return wireAcquisition.acquireWire().write(key);
    }

    @Override
    public ValueOut writeEvent(Class<?> expectedType, Object eventKey) throws InvalidMarshallableException {
        return wireAcquisition.acquireWire().writeEvent(expectedType, eventKey);
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

    @Override
    public DocumentContext acquireWritingDocument(boolean metaData) {
        return wireAcquisition.acquireWire().acquireWritingDocument(metaData);
    }

    @Override
    public String readingPeekYaml() {
        return wireAcquisition.acquireWire().readingPeekYaml();
    }

    /**
     * Strategy for discovering or creating the actual wire implementation.
     */
    interface WireAcquisition {

        /**
         * Returns a supplier describing the underlying {@link WireType}.
         */
        @NotNull
        Supplier<WireType> underlyingType();

        /**
         * Acquires the actual underlying wire instance.
         */
        @Nullable
        Wire acquireWire();

        /**
         * Sets the class lookup to use when creating the underlying wire.
         */
        void classLookup(ClassLookup classLookup);

        /**
         * Returns the class lookup associated with this acquisition.
         */
        @Nullable
        ClassLookup classLookup();
    }
}
