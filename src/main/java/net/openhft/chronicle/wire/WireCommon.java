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
import net.openhft.chronicle.bytes.HexDumpBytesDescription;
import net.openhft.chronicle.bytes.CommonMarshallable;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.values.*;
import net.openhft.chronicle.threads.Pauser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The WireCommon interface defines a common set of operations for managing
 * wire message configurations and states. It provides methods for handling
 * class lookups, blocking operations, accessing underlying bytes, manipulating
 * specific value types, and managing parent objects.
 * <p>
 * Implementations of this interface can customize their class lookup,
 * pausing, and marshalling strategies, as well as various settings such as
 * header numbers and padding.
 * <p>
 * The interface also provides methods to check whether the wire protocol
 * is binary, reset the state of the wire.
 */
public interface WireCommon {

    /**
     * Configures the {@link ClassLookup} implementation to be used for class lookups within this instance.
     *
     * @param classLookup implementation to handle class lookups.
     */
    void classLookup(ClassLookup classLookup);

    /**
     * Retrieves the currently configured {@link ClassLookup} implementation for class lookups.
     *
     * @return Current {@link ClassLookup} implementation.
     */
    ClassLookup classLookup();

    /**
     * Configures the {@link Pauser} implementation to be used for handling blocking operations within this instance.
     *
     * @param pauser Implementation to handle blocking operations.
     */
    void pauser(Pauser pauser);

    /**
     * Retrieves the currently configured {@link Pauser} implementation for handling blocking operations.
     *
     * @return Current {@link Pauser} implementation.
     */
    Pauser pauser();

    /**
     * Retrieves the underlying {@link Bytes} instance stored within this wire.
     *
     * @return Underlying {@link Bytes} instance.
     */
    @NotNull
    Bytes<?> bytes();

    /**
     * Provides a hexadecimal dump of the bytes, useful for comment or debugging purposes.
     *
     * @return Hexadecimal dump of the bytes.
     */
    HexDumpBytesDescription<?> bytesComment();

    /**
     * Creates and returns a new {@link IntValue}. The {@link IntValue} implementation that is
     * returned depends on the wire implementation.
     *
     * @return a new {@link IntValue}.
     */
    @NotNull
    IntValue newIntReference();

    /**
     * Creates and returns a new {@link LongValue}. The {@link LongValue} implementation that is
     * returned depends on the wire implementation.
     *
     * @return a new {@link LongValue}
     */
    @NotNull
    LongValue newLongReference();

    /**
     * Creates and returns a new {@link TwoLongValue}. The {@link TwoLongValue} implementation that
     * is returned depends on the wire implementation.
     *
     * @return a new {@link TwoLongValue}
     */
    @NotNull
    default TwoLongValue newTwoLongReference() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates and returns a new {@link LongArrayValues}. The {@link LongArrayValues} implementation that
     * is returned depends on the wire implementation.
     *
     * @return a new {@link LongArrayValues}
     */
    @NotNull
    LongArrayValues newLongArrayReference();

    /**
     * Creates and returns a new {@link IntArrayValues}. The {@link IntArrayValues} implementation that
     * is returned depends on the wire implementation.
     *
     * @return a new {@link IntArrayValues}
     */
    @NotNull
    IntArrayValues newIntArrayReference();

    /**
     * Resets the state of the underlying {@link Bytes} instance stored within this wire, allowing for reuse.
     */
    void clear();

    /**
     * Retrieves the parent object of this wire, if one has been assigned.
     *
     * @return Parent object of the wire or {@code null} if none was assigned.
     */
    @Nullable
    Object parent();

    /**
     * Assigns a parent object to this wire. This object can be retrieved later for contextual use.
     *
     * @param parent Parent object to assign, or null to unassign.
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
     * Sets a header number for the current wire message.
     *
     * @param headerNumber A long representing the header number.
     * @return the WireOut instance after setting the header number.
     */
    @NotNull
    WireOut headerNumber(long headerNumber);

    /**
     * Retrieves the current header number of the wire message.
     *
     * @return A long representing the current header number.
     */
    long headerNumber();

    /**
     * Enables or disables padding for the wire messages.
     *
     * @param usePadding A boolean value indicating whether to use padding.
     */
    void usePadding(boolean usePadding);

    /**
     * Retrieves the current setting for padding in wire messages.
     *
     * @return A boolean value indicating whether padding is currently being used.
     */
    boolean usePadding();

    /**
     * Creates and returns a new {@link BooleanValue}. The {@link BooleanValue} implementation that is
     * returned depends on the wire implementation.
     *
     * @return a new {@link BooleanValue}.
     */
    @NotNull
    BooleanValue newBooleanReference();

    /**
     * Sets whether the object should be written as a Marshallable or BytesMarshallable.
     *
     * @param object The object to check.
     * @return true if the object should be written as a Marshallable, false otherwise.
     */
    boolean useSelfDescribingMessage(@NotNull CommonMarshallable object);

    /**
     * Checks if the protocol of the wire is binary.
     *
     * @return true if the protocol is binary, false otherwise.
     */
    boolean isBinary();

    /**
     * Resets the state of the wire, allowing it to be reused.
     */
    void reset();
}