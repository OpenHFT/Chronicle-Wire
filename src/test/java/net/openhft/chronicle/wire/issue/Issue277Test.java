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

/**
 * Test class to validate behaviors associated with class aliases in the context of Wire.
 * This test extends the WireTestCommon for utility behaviors related to Wire tests.
 */
public class Issue277Test extends WireTestCommon {

    /**
     * Sets up the testing environment before executing the test methods.
     * It specifically adds class aliases to the ClassAliasPool.
     */
    @Before
    public void setup() {
        // Add class aliases for Data1 and Data2 to the ClassAliasPool
        ClassAliasPool.CLASS_ALIASES.addAlias(Data1.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(Data2.class);
    }

    // Sample data in string format to be used for deserialization tests
    static final String data = "" +
            "!Data1 {\n" +
            "  name: Tom,\n" +
            "  age: 25,\n" +
            "  address: \"21 high street, Liverpool\"\n" +
            "}\n";

    /**
     * Validates that the data can be correctly parsed into a Data2 object.
     * This test does not expect a RuntimeException because a correct class alias is provided.
     */
    @Test
    public void isOk() {
        // Deserialize the sample data into a Data2 object without throwing an exception
        Data2 o2 = WireType.TEXT.fromString(Data2.class, data);

        // Assert that the deserialized Data2 object matches the expected string representation
        assertEquals("!Data2 {\n" +
                "  name: Tom,\n" +
                "  age: 25,\n" +
                "  address: \"21 high street, Liverpool\"\n" +
                "}\n", o2.toString());
    }

    /**
     * Aims to reproduce a ClassCastException by trying to parse a Data1 serialized data
     * as if it was a Data2 serialized data without providing the class alias.
     */
    @Test(expected = ClassCastException.class)
    public void reproduce() {
        // This operation should fail and throw a ClassCastException
        Data2 o2 = WireType.TEXT.fromString(data);
        fail("" + o2);
    }

    /**
     * Sample data class representing a user's basic details.
     * This class extends the SelfDescribingMarshallable for serialization and deserialization.
     */
    private static class Data1 extends SelfDescribingMarshallable {
        String name;
        int age;
        String address;
    }

    /**
     * Another sample data class similar to Data1. Used to test the behaviors of class aliases.
     * Like Data1, this class also extends the SelfDescribingMarshallable.
     */
    private static class Data2 extends SelfDescribingMarshallable {
        String name;
        int age;
        String address;
    }
}
