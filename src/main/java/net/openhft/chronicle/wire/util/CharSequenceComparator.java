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

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public enum CharSequenceComparator implements Comparator<CharSequence> {
    INSTANCE;

    @Override
    public int compare(@NotNull CharSequence o1, @NotNull CharSequence o2) {
        int cmp = Integer.compare(o1.length(), o2.length());
        if (cmp != 0)
            return cmp;
        for (int i = 0, len = o1.length(); i < len; i++) {
            cmp = Character.compare(o1.charAt(i), o2.charAt(i));
            if (cmp != 0)
                return cmp;
        }
        return cmp;
    }
}
