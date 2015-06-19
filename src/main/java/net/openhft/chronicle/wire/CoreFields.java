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

import org.jetbrains.annotations.NotNull;

/**
 * Created by Rob Austin
 */
public enum CoreFields implements WireKey {
    tid,
    csp,
    cid,
    reply,
    exception;

    @NotNull
    private static StringBuilder eventName = new StringBuilder();

    private static long longEvent(@NotNull final WireKey expecting, @NotNull final WireIn wire) {
        final ValueIn valueIn = wire.readEventName(eventName);
        if (expecting.contentEquals(eventName))
            return valueIn.int64();

        throw new IllegalArgumentException("expecting a " + expecting);
    }

    @NotNull
    public static StringBuilder stringEvent(@NotNull final WireKey expecting, @NotNull StringBuilder using,
                                            @NotNull final WireIn wire) {
        final ValueIn valueIn = wire.readEventName(eventName);
        if (expecting.contentEquals(eventName)) {
            valueIn.textTo(using);
            return using;
        }

        throw new IllegalArgumentException("expecting a " + expecting);
    }

    public static long tid(@NotNull final WireIn wire) {
        return longEvent(CoreFields.tid, wire);
    }

    public static long cid(@NotNull final WireIn wire) {
        return longEvent(CoreFields.cid, wire);
    }

    private final static StringBuilder cpsBuilder = new StringBuilder();

    @NotNull
    public static StringBuilder csp(@NotNull final WireIn wire) {
        return stringEvent(CoreFields.csp, cpsBuilder, wire);
    }
}
