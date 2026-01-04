/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class JSONNanTest extends WireTestCommon {

    // Test to verify that a Dto object with Double.NaN as its value gets written as quoted "NaN" in JSON format
    @Test
    @DisplayName("Writes NaN as quoted string in json")
    public void writeNaNs() {
        // Allocate a new elastic byte buffer
        Bytes<?> b = Bytes.allocateElasticOnHeap();
        try {
            // Apply JSON wire type to the byte buffer
            Wire wire = WireType.JSON.apply(b);

            // Create a new Dto object and set its value field to Double.NaN
            Dto value = new Dto();
            value.value1 = Double.NaN;
            value.field = "text";

            // Write the Dto object to the wire
            wire.write().marshallable(value);

            // Assert that the wire content represents the Double.NaN as quoted "NaN" in JSON format
            Assertions.assertEquals("\"\":{\"value\":0.0,\"value1\":\"NaN\",\"value2\":0,\"field\":\"text\"}", wire.toString(),
                    "json output should quote NaN values");
        } finally {
            // Release the byte buffer resources
            b.releaseLast();
        }
    }

    // Test to verify that reading a JSON formatted null into a Dto object sets its value to Double.NaN
    @Test
    @DisplayName("Reads json null as NaN for double")
    public void readJSONNullToDoubleNaN() {
        Bytes<?> b = Bytes.from("\"\":{\"value\":null,\"value1\": null, \"value2\":\n0 ,\"field\": \"text\"}");
        Wire wire = WireType.JSON.apply(b);
        Dto value = wire.read().object(Dto.class);
        Assertions.assertTrue(Double.isNaN(value.value),
                "json null should parse to NaN for value");
    }

    // Test to verify that a leading space before the JSON formatted null is handled correctly
    @Test
    @DisplayName("Reads json null with leading space as NaN")
    public void readJSONNullWithLeadingSpaceToDoubleNaN() {
        Bytes<?> b = Bytes.from("\"\":{\"value\": null , \"field\" : \"text\" , \"value1\": 1\n,\n\"value2\": \"1\" \n}");
        Wire wire = WireType.JSON.apply(b);
        Dto value = wire.read().object(Dto.class);
        Assertions.assertTrue(Double.isNaN(value.value),
                "json null should parse to NaN for value with leading space");
        Assertions.assertEquals("text", value.field,
                "field should parse expected text value");
        Assertions.assertEquals(1.0, value.value1, 0.01,
                "value1 should parse numeric value from json");
        Assertions.assertEquals(1L, value.value2,
                "value2 should parse numeric value from json");
    }

    // Test to verify that reading a JSON formatted quoted "NaN" into a Dto object sets its value to Double.NaN
    @Test
    @DisplayName("Reads quoted NaN as NaN for doubles")
    public void readJSONQuotedNaNToNaN() {
        Bytes<?> b = Bytes.from("\"\":{\"value\":\"NaN\",\"value1\": \"NaN\" , \"value2\":\n0 ,\"field\": \"text\"}");
        Wire wire = WireType.JSON.apply(b);
        Dto value = wire.read().object(Dto.class);
        Assertions.assertTrue(Double.isNaN(value.value),
                "quoted NaN should parse to NaN for value");
        Assertions.assertTrue(Double.isNaN(value.value1),
                "quoted NaN should parse to NaN for value1");
    }

    // Class Dto extending SelfDescribingMarshallable with a single double field
    static class Dto extends SelfDescribingMarshallable {
        double value;
        double value1;
        long value2;
        String field;
    }
}
