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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class BytesUsageTest extends WireTestCommon {

    @SuppressWarnings("rawtypes")
    @Test
    public void testBytes() {
        BytesStore value = Bytes.from("helloWorld");
        {
            BytesWrapper bw = new BytesWrapper();
            bw.clOrdId(Bytes.from("A" + value));
            assertEquals(Bytes.from("AhelloWorld"), bw.clOrdId());
        }

        // gc free replacement
        BytesWrapper bw = new BytesWrapper(); // this should be recycled to avoid garbage

        bw.clOrdId().clear().append("A").append(value);
        assertEquals(Bytes.from("AhelloWorld"), bw.clOrdId());
        value.releaseLast();
    }

    @SuppressWarnings("rawtypes")
    static class BytesWrapper extends SelfDescribingMarshallable {
        Bytes<?> clOrdId = Bytes.allocateElasticOnHeap();

        public Bytes<?> clOrdId() {
            return clOrdId;
        }

        public BytesWrapper clOrdId(Bytes<?> clOrdId) {
            this.clOrdId = clOrdId;
            return this;
        }
    }
}
