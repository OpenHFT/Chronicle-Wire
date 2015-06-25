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

import net.openhft.chronicle.core.annotation.ForceInline;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by peter.lawrey on 1/10/15.
 */
@FunctionalInterface
public interface WireKey {

    @Nullable
    CharSequence name();

    @ForceInline
    default int code() {
        return name().toString().hashCode();
    }

    default Type type() {
        Object o = defaultValue();
        return o == null ? Void.class : o.getClass();
    }

    @Nullable
    default Object defaultValue() {
        return null;
    }

    static boolean checkKeys(@NotNull WireKey[] keys) {
        Map<Integer, WireKey> codes = new HashMap<>();
        for (WireKey key : keys) {
            WireKey pkey = codes.put(key.code(), key);
            if (pkey != null)
                throw new AssertionError(pkey + " and " + key + " have the same code " + key.code());
        }
        return true;
    }

    default boolean contentEquals(@NotNull CharSequence c){
        return this.toString().contentEquals(c);
    }
}
