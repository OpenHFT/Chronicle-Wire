/*
 *
 *  *     Copyright (C) 2016  higherfrequencytrading.com
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU Lesser General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU Lesser General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU Lesser General Public License
 *  *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package net.openhft.chronicle.wire;

/**
 * Created by peter.lawrey on 02/02/2016.
 */
class ValueInState {

    private static final long[] EMPTY_ARRAY = {};
    private long savedPosition;
    private int unexpectedSize;
    private long[] unexpected = EMPTY_ARRAY;

    public void reset() {
        savedPosition = 0;
        unexpectedSize = 0;
    }

    public void addUnexpected(long position) {
        if (unexpectedSize >= unexpected.length) {
            int newSize = unexpected.length * 3 / 2 + 8;
            long[] unexpected2 = new long[newSize];
            System.arraycopy(unexpected, 0, unexpected2, 0, unexpected.length);
            unexpected = unexpected2;
        }
        unexpected[unexpectedSize++] = position;
    }

    public void savedPosition(long savedPosition) {
        this.savedPosition = savedPosition;
    }

    public long savedPosition() {
        return savedPosition;
    }

    public int unexpectedSize() {
        return unexpectedSize;
    }

    public long unexpected(int index) {
        return unexpected[index];
    }

    public void removeUnexpected(int i) {
        int length = unexpectedSize - i - 1;
        if (length > 0)
            System.arraycopy(unexpected, i + 1, unexpected, i, length);
        unexpectedSize--;
    }
}
