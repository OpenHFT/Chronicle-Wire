/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

/*
 * Created by Peter Lawrey on 30/06/15.
 */
public interface WireCommon {

    /**
     * Define how classes should be looked up.
     *
     * @param classLookup to use
     */
    void classLookup(ClassLookup classLookup);

    /**
     * @return the current implementation for looking up classes.
     */
    ClassLookup classLookup();

    /**
     * @param pauser to use for blocking operations.
     */
    void pauser(Pauser pauser);

    /**
     * @return pauser used.
     */
    Pauser pauser();

    /**
     * @return the underlying Bytes
     */
    @NotNull
    Bytes<?> bytes();

    /**
     * @return the bytes90 but only for comment
     */
    BytesComment<?> bytesComment();

    /**
     * @return an IntValue which appropriate for this wire.
     */
    @NotNull
    IntValue newIntReference();

    /**
     * @return a LongValue which appropriate for this wire.
     */
    @NotNull
    LongValue newLongReference();

    /**
     * @return a LongValue which appropriate for this wire.
     */
    @NotNull
    default TwoLongValue newTwoLongReference() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return a LongArrayValue which appropriate for this wire.
     */
    @NotNull
    LongArrayValues newLongArrayReference();

    /**
     * reset the state of the current wire for reuse.
     */
    void clear();

    /**
     * Obtain the parent class of this wire if there is one
     *
     * @return the parent or null if none was assigned.
     */
    @Nullable
    Object parent();

    /**
     * Assign a parent object to this wire for later retrieval.
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

    /**
     * @return a BooleanValue which appropriate for this wire.
     */
    @NotNull
    BooleanValue newBooleanReference();
}
