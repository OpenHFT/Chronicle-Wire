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

package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class Issue277Test extends WireTestCommon {

    @Before
    public void setup() {
        // This is needed in order to reproduce the problem
        ClassAliasPool.CLASS_ALIASES.addAlias(Data1.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(Data2.class);
    }

    static final String data = "" +
            "!Data1 {\n" +
            "  name: Tom,\n" +
            "  age: 25,\n" +
            "  address: \"21 high street, Liverpool\"\n" +
            "}\n";

    @Test
    public void isOk() {
        // This doesn't throw a RuntimeException as Data2.class was provided
        Data2 o2 = WireType.TEXT.fromString(Data2.class, data);

        assertEquals("!Data2 {\n" +
                "  name: Tom,\n" +
                "  age: 25,\n" +
                "  address: \"21 high street, Liverpool\"\n" +
                "}\n", o2.toString());
    }

    @Test(expected = ClassCastException.class)
    public void reproduce() {
        // This throws a RuntimeException as Data2 != Data1
        Data2 o2 = WireType.TEXT.fromString(data);
        fail("" + o2);
    }

    private static class Data1 extends SelfDescribingMarshallable {
        String name;
        int age;
        String address;
    }

    private static class Data2 extends SelfDescribingMarshallable {
        String name;
        int age;
        String address;
    }

}
