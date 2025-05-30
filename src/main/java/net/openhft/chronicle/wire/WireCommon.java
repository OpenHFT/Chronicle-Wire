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
 * Defines the configuration hooks and utilities shared by every {@code Wire} implementation.
 * <p>
 * Methods cover class name resolution, pausing strategies, direct access to the underlying
 * {@link Bytes} and creation of primitive references that map onto the wire.
 */
public interface WireCommon {

    /**
     * Selects the {@link ClassLookup} used to resolve type names during deserialisation.
     */
    void classLookup(ClassLookup classLookup);

    /**
     * Returns the {@link ClassLookup} that resolves type names when reading from the wire.
     */
    ClassLookup classLookup();

    /**
     * Sets the {@link Pauser} used when a read or write must wait for more data.
     */
    void pauser(Pauser pauser);

    /**
     * Returns the {@link Pauser} that controls blocking behaviour.
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
     * Returns a secondary {@link Bytes} view used for commentary when producing
     * hex dumps.
     */
    HexDumpBytesDescription<?> bytesComment();

    /**
     * Creates a wire-backed {@link IntValue} for direct integer access.
     */
    @NotNull
    IntValue newIntReference();

    /**
     * Creates a wire-backed {@link LongValue} for low-latency long fields.
     */
    @NotNull
    LongValue newLongReference();

    /**
     * Creates a wire-backed {@link TwoLongValue}. Unsupported by some text wires.
     */
    @NotNull
    default TwoLongValue newTwoLongReference() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a wire-backed {@link LongArrayValues} for direct array access.
     */
    @NotNull
    LongArrayValues newLongArrayReference();

    /**
     * Creates a wire-backed {@link IntArrayValues} for low-level array work.
     */
    @NotNull
    IntArrayValues newIntArrayReference();

    /**
     * Clears the underlying {@link Bytes} and resets wire state.
     */
    void clear();

    /**
     * Returns the parent object if one has been associated with this wire.
     */
    @Nullable
    Object parent();

    /**
     * Associates a parent object with this wire.
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
     * Sets the sequence number for the next header and returns the wire.
     */
    @NotNull
    WireOut headerNumber(long headerNumber);

    /**
     * Returns the current header sequence number.
     */
    long headerNumber();

    /**
     * Enables or disables padding between documents for alignment.
     */
    void usePadding(boolean usePadding);

    /**
     * Returns whether padding is currently enabled.
     */
    boolean usePadding();

    /**
     * Creates a wire-backed {@link BooleanValue}.
     */
    @NotNull
    BooleanValue newBooleanReference();

    /**
     * Indicates whether the given object should write its type information into the wire.
     * Typically used with {@link net.openhft.chronicle.wire.SelfDescribingMarshallable}.
     */
    boolean useSelfDescribingMessage(@NotNull CommonMarshallable object);

    /**
     * Determine whether direct access to the underlying byte() makes sense or should it be treated as text
     *
     * @return Is this a binary protocol
     */
    boolean isBinary();

    /**
     * Reinitialises the wire and clears any cached state.
     */
    void reset();
}
