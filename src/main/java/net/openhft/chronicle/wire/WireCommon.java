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
import net.openhft.chronicle.bytes.CommonMarshallable;
import net.openhft.chronicle.bytes.HexDumpBytesDescription;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.values.*;
import net.openhft.chronicle.threads.Pauser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Common configuration and factory methods shared by all wire types.
 * These allow a wire to control class resolution, pausing behaviour and
 * creation of low-latency value objects.
 */
public interface WireCommon {

    /**
     * Sets the {@link ClassLookup} used to resolve class names during
     * deserialisation. This comes into play when type aliases or numeric
     * identifiers are read from the wire.
     *
     * @param classLookup strategy for resolving class names
     */
    void classLookup(ClassLookup classLookup);

    /**
     * Returns the {@link ClassLookup} currently in use for resolving class
     * names during deserialisation.
     */
    ClassLookup classLookup();

    /**
     * Sets the {@link Pauser} controlling how read operations wait for data.
     * Typical implementations yield or park the thread when the wire is empty.
     *
     * @param pauser policy for blocking behaviour
     */
    void pauser(Pauser pauser);

    /**
     * Returns the {@link Pauser} used to manage blocking behaviour.
     */
    Pauser pauser();

    /**
     * Returns the underlying {@link Bytes} stored by the wire.
     *
     * @return the underlying {@link Bytes} stored by the wire
     */
    @NotNull
    Bytes<?> bytes();

    /**
     * Returns the underlying {@link Bytes} for use in diagnostics. Implementati
     * ons can attach textual comments to these bytes when producing hex dumps so
     * that the main data stream is unaffected.
     */
    HexDumpBytesDescription<?> bytesComment();

    /**
     * Creates an integer value bound to this wire. The concrete return type
     * depends on whether the wire is text or binary.
     */
    @NotNull
    IntValue newIntReference();

    /**
     * Creates a long value bound to this wire.
     */
    @NotNull
    LongValue newLongReference();

    /**
     * Creates two long values stored directly in the wire.
     */
    @NotNull
    default TwoLongValue newTwoLongReference() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates an array of long values mapped to the wire for low latency updates.
     */
    @NotNull
    LongArrayValues newLongArrayReference();

    /**
     * Creates an array of int values mapped to the wire for low latency updates.
     */
    @NotNull
    IntArrayValues newIntArrayReference();

    /**
     * Clears the wire and resets the underlying {@link Bytes} positions.
     */
    void clear();

    /**
     * Returns the wire parent object used for nested marshallable structures.
     * May be {@code null} if no parent has been set.
     */
    @Nullable
    Object parent();

    /**
     * Assigns a parent object for later retrieval.
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

    default void notCompleteIsNotPresent(boolean notCompleteArePresent) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the header sequence number. This is typically used by queue
     * implementations to track message order.
     */
    @NotNull
    WireOut headerNumber(long headerNumber);

    /**
     * Returns the current header sequence number.
     */
    long headerNumber();

    /**
     * Enables or disables padding for alignment purposes.
     */
    void usePadding(boolean usePadding);

    /**
     * Returns whether padding is enabled.
     */
    boolean usePadding();

    /**
     * Creates a boolean value mapped directly to the underlying bytes.
     */
    @NotNull
    BooleanValue newBooleanReference();

    /**
     * Returns {@code true} if the given object should write its type
     * information into the wire (see {@code SelfDescribingMarshallable}).
     */
    boolean useSelfDescribingMessage(@NotNull CommonMarshallable object);

    /**
     * Returns {@code true} if this wire uses a binary format rather than text.
     */
    boolean isBinary();

    /**
     * Resets the wire to its initial state, clearing positions and internal
     * buffers.
     */
    void reset();
}
