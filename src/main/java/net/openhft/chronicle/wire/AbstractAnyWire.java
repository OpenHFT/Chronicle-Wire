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

import java.util.Objects;
import java.util.function.Supplier;

/**
 * An abstract representation of a wire type that could be either {@code TextWire} or {@code BinaryWire}.
 * This class provides a generic foundation for wire types that could shift between the two mentioned types
 * based on the underlying acquisition logic.
 *
 * @author Rob Austin.
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractAnyWire extends AbstractWire implements Wire {

    @NotNull
    protected final WireAcquisition wireAcquisition;  // Responsible for acquiring the actual wire type (TextWire or BinaryWire).

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
     * Retrieves the underlying wire, which could be either {@code TextWire} or {@code BinaryWire},
     * based on the acquisition strategy.
     *
     * @return The underlying wire type.
     */
    @Nullable
    public Wire underlyingWire() {
        return requireWire();
    }

    /**
     * Provides a supplier that indicates the type of the underlying wire.
     *
     * @return A supplier yielding the {@code WireType}.
     */
    @NotNull
    public Supplier<WireType> underlyingType() {
        return wireAcquisition.underlyingType();
    }

    private Wire requireWire() {
        return Objects.requireNonNull(wireAcquisition.acquireWire(), "wireAcquisition returned null");
    }

    @Override
    public void copyTo(@NotNull WireOut wire) throws InvalidMarshallableException {
        requireWire().copyTo(wire);
    }

    @NotNull
    @Override
    public ValueIn read() {
        return requireWire().read();
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull WireKey key) {
        return requireWire().read(key);
    }

    @NotNull
    @Override
    public ValueIn read(@NotNull StringBuilder name) {
        return requireWire().read(name);
    }

    @Nullable
    @Override
    public <K> K readEvent(Class<K> expectedClass) throws InvalidMarshallableException {
        return requireWire().readEvent(expectedClass);
    }

    @Override
    public void writeStartEvent() {
        requireWire().writeStartEvent();
    }

    @Override
    public void writeEndEvent() {
        requireWire().writeEndEvent();
    }

    @NotNull
    @Override
    public ValueIn getValueIn() {
        return requireWire().getValueIn();
    }

    @NotNull
    @Override
    public WireIn readComment(@NotNull StringBuilder sb) {
        return requireWire().readComment(sb);
    }

    @NotNull
    @Override
    public IntValue newIntReference() {
        return requireWire().newIntReference();
    }

    @NotNull
    @Override
    public LongValue newLongReference() {
        return requireWire().newLongReference();
    }

    @NotNull
    @Override
    public LongArrayValues newLongArrayReference() {
        return requireWire().newLongArrayReference();
    }

    @Override
    public @NotNull IntArrayValues newIntArrayReference() {
        return requireWire().newIntArrayReference();
    }

    void checkWire() {
        requireWire();
    }

    @NotNull
    @Override
    public DocumentContext readingDocument() {
        return requireWire().readingDocument();
    }

    @Override
    public DocumentContext readingDocument(long readLocation) {
        return requireWire().readingDocument(readLocation);
    }

    @Override
    public void consumePadding() {
        final Wire wire = requireWire();
        wire.commentListener(commentListener);
        wire.consumePadding();
    }

    @NotNull
    @Override
    public ValueOut write() {
        return requireWire().write();
    }

    @NotNull
    @Override
    public ValueOut write(WireKey key) {
        return requireWire().write(key);
    }

    @Override
    public ValueOut write(CharSequence key) {
        return requireWire().write(key);
    }

    @Override
    public ValueOut writeEvent(Class<?> expectedType, Object eventKey) throws InvalidMarshallableException {
        return requireWire().writeEvent(expectedType, eventKey);
    }

    @NotNull
    @Override
    public ValueOut getValueOut() {
        return requireWire().getValueOut();
    }

    @NotNull
    @Override
    public WireOut writeComment(CharSequence s) {
        return requireWire().writeComment(s);
    }

    @NotNull
    @Override
    public WireOut addPadding(int paddingToAdd) {
        return requireWire().addPadding(paddingToAdd);
    }

    @Override
    public DocumentContext writingDocument(boolean metaData) {
        return requireWire().writingDocument(metaData);
    }

    @Override
    public DocumentContext acquireWritingDocument(boolean metaData) {
        return requireWire().acquireWritingDocument(metaData);
    }

    @Override
    public String readingPeekYaml() {
        return requireWire().readingPeekYaml();
    }

    /**
     * Represents an interface defining the strategy for acquiring and interacting with the underlying wire types.
     */
    interface WireAcquisition {

        /**
         * Provides a supplier indicating the type of the underlying wire, which could be either {@code TextWire} or {@code BinaryWire}.
         *
         * @return A supplier yielding the {@code WireType}.
         */
        @NotNull
        Supplier<WireType> underlyingType();

        /**
         * Retrieves the actual wire type which could be either {@code TextWire} or {@code BinaryWire}.
         *
         * @return The acquired wire type.
         */
        @Nullable
        Wire acquireWire();

        /**
         * Sets the class lookup mechanism for this wire acquisition.
         *
         * @param classLookup The class lookup mechanism to set.
         */
        void classLookup(ClassLookup classLookup);

        /**
         * Retrieves the class lookup mechanism associated with this wire acquisition.
         *
         * @return The class lookup mechanism.
         */
        @Nullable
        ClassLookup classLookup();
    }
}
