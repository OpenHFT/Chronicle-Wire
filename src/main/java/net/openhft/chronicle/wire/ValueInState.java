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

import org.jetbrains.annotations.NotNull;

/**
 * Created by peter.lawrey on 02/02/2016.
 */
class ValueInState {

    private static final long[] EMPTY_ARRAY = {};
    private long savedPosition;
    private int unexpectedSize;
    @NotNull
    private long[] unexpected = EMPTY_ARRAY;

    public void reset() {
        savedPosition = 0;
        unexpectedSize = 0;
    }

    public void addUnexpected(long position) {
        if (unexpectedSize >= unexpected.length) {
            int newSize = unexpected.length * 3 / 2 + 8;
            @NotNull long[] unexpected2 = new long[newSize];
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
