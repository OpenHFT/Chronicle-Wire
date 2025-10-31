/*
 * Copyright 2016-2025 chronicle.software
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
import org.junit.Test;

import static org.junit.Assert.*;

public class VanillaMessageHistoryRoundTripTest extends WireTestCommon {

    @Test
    public void roundTripViaWire() {
        VanillaMessageHistory mh = new VanillaMessageHistory();
        mh.addSource(1, 11L);
        mh.addTiming(123L);

        Wire w = WireType.BINARY.apply(Bytes.allocateElasticOnHeap(256));
        w.write("mh").object(mh);
        VanillaMessageHistory r = w.read("mh").object(VanillaMessageHistory.class);

        assertEquals(mh.timings(), r.timings());
        assertEquals(mh.timing(0), r.timing(0));
    }

    @Test
    public void roundTripViaBytes() {
        VanillaMessageHistory mh = new VanillaMessageHistory();
        mh.addSource(2, 22L);
        mh.addTiming(456L);

        Bytes<?> b = Bytes.allocateElasticOnHeap(128);
        mh.writeMarshallable(b);
        assertTrue(b.readRemaining() > 0);

        VanillaMessageHistory r = new VanillaMessageHistory();
        r.readMarshallable(b);
        assertEquals(mh.timings(), r.timings());
        assertEquals(mh.timing(0), r.timing(0));
    }
}

