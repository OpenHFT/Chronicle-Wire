/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.CommonMarshallable;
import net.openhft.chronicle.bytes.HexDumpBytesDescription;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.values.*;
import net.openhft.chronicle.threads.Pauser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Common operations and configuration hooks shared by Chronicle Wire implementations.
 * <p>
 * Implementations provide access to the underlying {@link Bytes}, reference value factories,
 * class lookup and parent relationships and expose knobs such as padding, binary vs text mode and
 * self describing message behaviour.
 */
public interface WireCommon {

    /**
     * Sets the {@link ClassLookup} implementation to be used for class lookup.
     *
     * @param classLookup implementation to be used for class lookup.
     */
    void classLookup(ClassLookup classLookup);

    /**
     * Returns the current {@link ClassLookup} implementation being used for class lookup.
     *
     * @return the current {@link ClassLookup} implementation being used for class lookup
     */
    ClassLookup classLookup();

    /**
     * Sets the {@link Pauser} implementation to be used for blocking operations.
     *
     * @param pauser implementation to be used for blocking operations.
     */
    void pauser(Pauser pauser);

    /**
     * Returns the current {@link Pauser} implementation being used for blocking operations.
     *
     * @return the current {@link Pauser} implementation being used for blocking operations
     */
    @Deprecated(/* to be removed in 2027 */)
    Pauser pauser();

    /**
     * Returns the underlying {@link Bytes} stored by the wire.
     *
     * @return the underlying {@link Bytes} stored by the wire
     */
    @NotNull
    Bytes<?> bytes();

    /**
     * Returns a bytes description used for comment rendering.
     *
     * @return the bytes() but only for comment
     */
    HexDumpBytesDescription<?> bytesComment();

    /**
     * Creates a new IntValue reference backed by the wire implementation.
     *
     * @return a new {@link IntValue}.
     */
    @NotNull
    IntValue newIntReference();

    /**
     * Creates a new LongValue reference backed by the wire implementation.
     *
     * @return a new {@link LongValue}
     */
    @NotNull
    LongValue newLongReference();

    /**
     * Creates a new TwoLongValue reference backed by the wire implementation.
     *
     * @return a new {@link TwoLongValue}
     */
    @NotNull
    default TwoLongValue newTwoLongReference() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new LongArrayValues reference backed by the wire implementation.
     *
     * @return a new {@link LongArrayValues}
     */
    @NotNull
    LongArrayValues newLongArrayReference();

    /**
     * Creates a new IntArrayValues reference backed by the wire implementation.
     *
     * @return a new {@link IntArrayValues}
     */
    @NotNull
    @Deprecated(/* to be removed in 2027 */)
    IntArrayValues newIntArrayReference();

    /**
     * Resets the state of the underlying {@link Bytes} stored by the wire.
     */
    void clear();

    /**
     * Returns the wire parent object. If the parent was not assigned, {@code null} is
     * returned instead.
     *
     * @return the wire parent object or {@code null} if none was assigned.
     */
    @Nullable
    Object parent();

    /**
     * Assigns the wire parent object for later retrieval.
     *
     * @param parent to set, or null if there isn't one.
     */
    void parent(Object parent);

    /**
     * If a message is marked as NOT_COMPLETE is it still present.
     *
     * @return true if NOT_COMPLETE messages can be seen, false if they cannot.
     */
    default boolean notCompleteIsNotPresent() {
        return true;
    }

    /**
     * Configure whether documents marked as NOT_COMPLETE should remain visible to readers.
     *
     * @param notCompleteArePresent true to expose incomplete messages, false to hide them
     */
    @Deprecated(/* to be removed in 2027 */)
    default void notCompleteIsNotPresent(boolean notCompleteArePresent) {
        throw new UnsupportedOperationException();
    }

    /**
     * Set the header number to write with the next document.
     *
     * @param headerNumber header to assign
     * @return this wire for chaining
     */
    @NotNull
    WireOut headerNumber(long headerNumber);

    /**
     * Current header number in use for the wire stream.
     *
     * @return header number or zero when unset
     */
    long headerNumber();

    /**
     * Enable or disable insertion of padding bytes between documents.
     *
     * @param usePadding true to align documents with padding
     */
    void usePadding(boolean usePadding);

    /**
     * Whether the wire currently uses padding to align documents.
     *
     * @return true if padding is enabled
     */
    boolean usePadding();

    /**
     * Creates a new BooleanValue reference backed by the wire implementation.
     *
     * @return a new {@link BooleanValue}.
     */
    @NotNull
    @Deprecated(/* to be removed in 2027 */)
    BooleanValue newBooleanReference();

    /**
     * Should this wire write the object as a Marshallable or BytesMarshallable
     *
     * @param object candidate marshallable to test
     * @return use Marshallable
     */
    boolean useSelfDescribingMessage(@NotNull CommonMarshallable object);

    /**
     * Determine whether direct access to the underlying byte() makes sense or should it be treated as text
     *
     * @return Is this a binary protocol
     */
    boolean isBinary();

    /**
     * Resets the internal state of the wire implementation.
     */
    void reset();
}
