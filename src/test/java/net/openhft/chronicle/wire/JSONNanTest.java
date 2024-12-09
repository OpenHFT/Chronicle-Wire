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

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

public class JSONNanTest extends WireTestCommon {

    // Test to verify that a Dto object with Double.NaN as its value gets written as null in JSON format
    @Test
    public void writeNaNs() {
        // Allocate a new elastic byte buffer
        Bytes<?> b = Bytes.elasticByteBuffer();
        try {
            // Apply JSON wire type to the byte buffer
            Wire wire = WireType.JSON.apply(b);

            // Create a new Dto object and set its value field to Double.NaN
            Dto value = new Dto();
            value.value1 = Double.NaN;
            value.field = "text";

            // Write the Dto object to the wire
            wire.write().marshallable(value);

            // Assert that the wire content represents the NaN as null
            Assert.assertEquals("\"\":{\"value\":0.0,\"value1\":null,\"value2\":0,\"field\":\"text\"}", wire.toString());
        } finally {
            // Release the byte buffer resources
            b.releaseLast();
        }
    }

    // Test to verify that reading a JSON formatted null into a Dto object sets its value to Double.NaN
    @Test
    public void readNaNs() {
        Bytes<?> b = Bytes.from("\"\":{\"value\":null,\"value1\": null, \"value2\":\n0 ,\"field\": \"text\"}");
        Wire wire = WireType.JSON.apply(b);
        Dto value = wire.read().object(Dto.class);
        Assert.assertTrue(Double.isNaN(value.value));
    }

    // Test to verify that a leading space before the JSON formatted null is handled correctly
    @Test
    public void readNaNWithFieldNext() {
        Bytes<?> b = Bytes.from("\"\":{\"value\": null , \"field\" : \"text\" , \"value1\": 1\n,\n\"value2\": \"1\" \n}");
        Wire wire = WireType.JSON.apply(b);
        Dto value = wire.read().object(Dto.class);
        Assert.assertTrue(Double.isNaN(value.value));
        Assert.assertEquals("text", value.field);
        Assert.assertEquals(1.0, value.value1, 0.01);
        Assert.assertEquals(1L, value.value2);
    }

    // Class Dto extending SelfDescribingMarshallable with a single double field
    public static class Dto extends SelfDescribingMarshallable {
        double value;
        double value1;
        long value2;
        String field;
    }
}
