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
package net.openhft.chronicle.wire.util;

import net.openhft.chronicle.core.annotation.ForceInline;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public enum CharSequenceComparator implements Comparator<CharSequence> {
    INSTANCE;

    @Override
    @ForceInline
    public int compare(@NotNull CharSequence o1, @NotNull CharSequence o2) {
        final int o1Length = o1.length();
        final int o2Length = o2.length();
        final int len = Math.min(o1Length, o2Length);
        for (int i = 0; i < len; i++) {
            final int cmp = Character.compare(o1.charAt(i), o2.charAt(i));
            if (cmp != 0)
                return cmp;
        }
        return Integer.compare(o1Length, o2Length);
    }
}
