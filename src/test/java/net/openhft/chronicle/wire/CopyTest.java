/*
 * Copyright 2016 higherfrequencytrading.com
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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class CopyTest {
    private final WireType from, to;
    private boolean withType;

    public CopyTest(WireType from, WireType to, boolean withType) {
        this.from = from;
        this.to = to;
        this.withType = withType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
//                new Object[] {WireType.TEXT, WireType.BINARY, true}, // not supported yet
//                new Object[] {WireType.TEXT, WireType.BINARY, false}, // not supported yet
                new Object[]{WireType.BINARY, WireType.TEXT, true},
                new Object[]{WireType.BINARY, WireType.TEXT, false}
        );
    }

    @Test
    public void testCopy() {
        Bytes bytesFrom = Bytes.elasticHeapByteBuffer(64);
        Wire wireFrom = from.apply(bytesFrom);
        Bytes bytesTo = Bytes.elasticHeapByteBuffer(64);
        Wire wireTo = to.apply(bytesTo);

        AClass a = create();
        if (withType)
            wireFrom.getValueOut().object(a);
        else
            wireFrom.getValueOut().marshallable(a);

        wireFrom.copyTo(wireTo);
        AClass b;
        if (withType)
            b = (AClass) wireTo.getValueIn().object();
        else
            b = wireTo.getValueIn().object(AClass.class);

        assertEquals(a, b);
    }

    private AClass create() {
        AClass aClass = new AClass();
        aClass.map = new HashMap<>();
        aClass.map.put(CcyPair.EURUSD, "eurusd");
        aClass.array = new String[]{"hello", "there"};
        aClass.intValue = 11;
        aClass.value = 123.4;
        return aClass;
    }

    private static class AClass extends AbstractMarshallable {
        Map<CcyPair, String> map;
        String[] array;
        int intValue;
        double value;
    }
}
