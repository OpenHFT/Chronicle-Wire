/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
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
 * Common functionality shared by all {@link Wire} implementations.
 * <p>
 * This interface groups configuration options and utility factory methods that
 * are required by both {@link WireIn} and {@link WireOut}. Implementations
 * use these methods to control how marshalling is performed, how waiting or
 * back pressure is handled and to create value references bound to the
 * underlying wire format.
 */
public interface WireCommon {

    /**
     * Sets the {@link ClassLookup} used to resolve class names during
     * deserialization.  This is particularly important when type information is
     * written as a textual alias or when reading a {@code class} field from the
     * wire.
     *
     * @param classLookup strategy used for class resolution
     */
    void classLookup(ClassLookup classLookup);

    /**
     * Returns the current {@link ClassLookup} used for resolving class names
     * while reading objects from the wire.
     *
     * @return the {@link ClassLookup} in use
     */
    ClassLookup classLookup();

    /**
     * Sets the {@link Pauser} implementation.  A {@code Pauser} defines the
     * strategy used by blocking operations to wait for data (for example,
     * busy-spin, yield or park).
     *
     * @param pauser strategy for blocking/waiting operations
     */
    void pauser(Pauser pauser);

    /**
     * Returns the {@link Pauser} used when blocking for more data or capacity.
     *
     * @return the pauser strategy
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
     * Returns the underlying bytes when generating debug output such as hex
     * dumps.  Comments written via {@link ValueOut#comment(CharSequence)} are
     * directed here so the primary data stream is unaffected.
     */
    HexDumpBytesDescription<?> bytesComment();

    /**
     * Creates a direct {@link IntValue} bound to this wire.  The concrete
     * implementation (binary or text) depends on the underlying wire type and
     * allows low latency access to an integer field stored within the wire.
     */
    @NotNull
    IntValue newIntReference();

    /**
     * Creates a direct {@link LongValue} bound to this wire.  As with
     * {@link #newIntReference()}, the actual implementation is wire specific and
     * provides direct access to a long field within the wire data.
     */
    @NotNull
    LongValue newLongReference();

    /**
     * Creates a new {@link TwoLongValue} reference.  The returned instance gives
     * low level access to two consecutive long values stored in the wire.
     * Concrete implementation is wire specific.
     */
    @NotNull
    default TwoLongValue newTwoLongReference() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates an array view over long values stored in the wire.  Useful for
     * accessing memory-mapped arrays directly without additional copying.
     */
    @NotNull
    LongArrayValues newLongArrayReference();

    /**
     * Creates an array view over integer values stored in the wire.  The
     * concrete implementation depends on the wire type.
     */
    @NotNull
    IntArrayValues newIntArrayReference();

    /**
     * Clears the underlying {@link Bytes} and resets any internal state held by
     * the wire implementation.
     */
    void clear();

    /**
     * Returns the parent object associated with this wire.  This is typically
     * used for nested marshallable objects to access their containing object.
     *
     * @return the parent object or {@code null} if none was set
     */
    @Nullable
    Object parent();

    /**
     * Assigns a parent object which may later be retrieved via
     * {@link #parent()}.  This allows wires to maintain a hierarchy of
     * marshallable objects if required.
     *
     * @param parent the parent object or {@code null}
     */
    void parent(Object parent);

    /**
     * Whether a message flagged as {@link Wires#NOT_COMPLETE} should still be
     * treated as visible when reading.
     *
     * @return {@code true} if incomplete messages appear as absent
     */
    default boolean notCompleteIsNotPresent() {
        return true;
    }

    default void notCompleteIsNotPresent(boolean notCompleteArePresent) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    WireOut headerNumber(long headerNumber);

    long headerNumber();

    /**
     * Enables or disables padding around messages.  Padding can be used to
     * align messages on cache-line boundaries to improve performance.
     */
    void usePadding(boolean usePadding);

    /**
     * @return {@code true} if padding is enabled for read and write operations
     */
    boolean usePadding();

    /**
     * Creates a {@link BooleanValue} bound to this wire for direct manipulation
     * of an on-wire boolean value.
     */
    @NotNull
    BooleanValue newBooleanReference();

    /**
     * Determines whether the provided object should write its own class type
     * when being marshalled.  Implementations may choose to omit the type if it
     * can be inferred from context.
     *
     * @return {@code true} if the object should use a self describing message
     */
    boolean useSelfDescribingMessage(@NotNull CommonMarshallable object);

    /**
     * Determines whether the underlying wire format is binary.  Binary wires are
     * optimised for speed and size whereas text wires favour readability.
     */
    boolean isBinary();

    /**
     * Reset any internal read/write positions or temporary state held by the
     * wire.
     */
    void reset();
}
