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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter.lawrey on 02/02/2016.
 */
class ValueInStack {
    final List<ValueInState> freeList = new ArrayList<>();
    int level = 0;

    public void reset() {
        level = 0;
    }

    public void push() {
        level++;
        if (freeList.size() > level) {
            freeList.get(level).reset();
        }
    }

    public void pop() {
        if (level < 0)
            throw new IllegalStateException();
        level--;
    }

    public ValueInState curr() {
        while (freeList.size() <= level)
            freeList.add(new ValueInState());
        return freeList.get(level);
    }
}
