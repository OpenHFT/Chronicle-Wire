/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
        return wireAcquisition.acquireWire();
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

        // CSClassLookupExposure REVIEW keep void classLookup(ClassLookup classLookup) here because this class-lookup surface in WireAcquisition#classLookup still needs either a closed alias registry or an explicit reviewed class-resolution contract.
        /**
         * Sets the class lookup mechanism for this wire acquisition.
         *
         * @param classLookup The class lookup mechanism to set.
         */
        void classLookup(ClassLookup classLookup);

        // CSClassLookupExposure REVIEW keep @Nullable here because this class-lookup surface in WireAcquisition#classLookup.
        /**
         * Retrieves the class lookup mechanism associated with this wire acquisition.
         *
         * @return The class lookup mechanism.
         */
        @Nullable
        ClassLookup classLookup();
    }
}
