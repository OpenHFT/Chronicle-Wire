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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@Ignore("TODO: fix copying for both TextWire and BinaryWire")
@RunWith(value = Parameterized.class)
public class CopyTest {
    private final WireType from, to;

    public CopyTest(WireType from, WireType to) {
        this.from = from;
        this.to = to;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[] {WireType.TEXT, WireType.BINARY},
                new Object[] {WireType.BINARY, WireType.TEXT}
        );
    }

    @Test
    public void testCopy() {
        Bytes bytesFrom = Bytes.elasticByteBuffer();
        Wire wireFrom = from.apply(bytesFrom);
        Bytes bytesTo = Bytes.elasticByteBuffer();
        Wire wireTo = to.apply(bytesTo);

        AClass a = create();
        wireFrom.getValueOut().marshallable(a);

        wireFrom.copyTo(wireTo);
        AClass b = (AClass) wireTo.getValueIn().object();

        assertEquals(a, b);
    }

    private AClass create() {
        AClass aClass = new AClass();
        aClass.map = new HashMap<>();
        aClass.map.put(CcyPair.EURUSD, "eurusd");
        aClass.array = new String[] { "hello", "there" };
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
