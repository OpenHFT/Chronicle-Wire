/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.threads.Pauser;
import org.jetbrains.annotations.NotNull;

/**
 * Created by peter on 30/06/15.
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
     * @return a LongArrayValue which appropriate for this wire.
     */
    @NotNull
    LongArrayValues newLongArrayReference();

    /**
     * reset the state of the current wire for reuse.
     */
    void clear();

    int sourceId();

    Wire sourceId(int sourceId);

    long sourceIndex();

    Wire sourceIndex(long sourceIndex);
}
