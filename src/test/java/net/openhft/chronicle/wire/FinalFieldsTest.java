/*
 * Copyright 2016-2020 chronicle.software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class FinalFieldsTest extends WireTestCommon {
    @SuppressWarnings("rawtypes")
    @Test
    public void testCopy() {
        expectException("Overwriting final field map");
        expectException("Overwriting final field array");
        expectException("Overwriting final field intValue");
        expectException("Overwriting final field value");

        Bytes bytesFrom = Bytes.allocateElasticOnHeap(64);
        Wire wireFrom = WireType.BINARY.apply(bytesFrom);
        Bytes bytesTo = Bytes.allocateElasticOnHeap(64);
        Wire wireTo = WireType.JSON.apply(bytesTo);

        FinalFieldsClass a = create();

        wireFrom.getValueOut().marshallable(a);

        wireFrom.copyTo(wireTo);
        FinalFieldsClass b = wireTo.getValueIn().object(FinalFieldsClass.class);

        assertEquals(a, b);
    }

    private FinalFieldsClass create() {
        Map<CcyPair, String> map = new HashMap<>();
        map.put(CcyPair.EURUSD, "eurusd");
        return new FinalFieldsClass(map, new String[]{"hello", "there"}, 11, 123.4);
    }

    @SuppressWarnings("unused")
    private static class FinalFieldsClass extends SelfDescribingMarshallable {
        final Map<CcyPair, String> map;
        final String[] array;
        final int intValue;
        final double value;

        public FinalFieldsClass(Map<CcyPair, String> map, String[] array, int intValue, double value) {
            this.map = map;
            this.array = array;
            this.intValue = intValue;
            this.value = value;
        }
    }
}
