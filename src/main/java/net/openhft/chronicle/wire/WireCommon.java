/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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
import net.openhft.chronicle.bytes.BytesComment;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.values.*;
import net.openhft.chronicle.threads.Pauser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    Pauser pauser();

    /**
     * Returns the underlying {@link Bytes} stored by the wire.
     *
     * @return the underlying {@link Bytes} stored by the wire
     */
    @NotNull
    Bytes<?> bytes();

    /**
     * Returns the bytes() but only for comment.
     *
     * @return the bytes() but only for comment
     */
    BytesComment<?> bytesComment();

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
     * Used to check that the wire is being used by only one thread
     *
     * @return true always, so it can be used in an assert line
     */
    boolean startUse();

    /**
     * Check the Wire was not used by another thread.
     *
     * @return true always, so it can be used in an assert line
     */
    boolean endUse();

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

    @NotNull
    WireOut headerNumber(long headerNumber);

    long headerNumber();

    void usePadding(boolean usePadding);

    boolean usePadding();

    /**
     * Creates and returns a new {@link BooleanValue}. The {@link BooleanValue} implementation that is
     * returned depends on the wire implementation.
     *
     * @return a new {@link BooleanValue}.
     */
    @NotNull
    BooleanValue newBooleanReference();
}