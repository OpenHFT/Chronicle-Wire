/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
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

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.ref.BinaryIntArrayReference;
import net.openhft.chronicle.bytes.ref.BinaryLongArrayReference;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.values.IntArrayValues;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;

public class TwoArrays extends SelfDescribingMarshallable implements Closeable {
    final IntArrayValues ia;
    final LongArrayValues la;
    transient boolean closed;

    public TwoArrays(int isize, long lsize) {
        this.ia = new BinaryIntArrayReference(isize);
        this.la = new BinaryLongArrayReference(lsize);
    }

    @Override
    public void close() {
        Closeable.closeQuietly(ia, la);
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}
