/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// CSVWireTest class extends from WireTestCommon and tests functionality related to CSV-based wire processing.
@SuppressWarnings({"deprecation", "removal"})
public class CSVWireTest extends WireTestCommon {

    // Test parsing a CSV string into a wire and reading its contents.
    @Test
    @DisplayName("CSV wire reads headings and data rows")
    public void testFrom() {
        // Create a Wire object from the given CSV string.
        @NotNull Wire wire = CSVWire.from(
                "heading1, heading2,heading3\n" +
                        "data1, data2, \"data three\"\n" +
                        "row2, row2b, row2c\n");
        // Ensure wire has content to read.
        assertTrue(wire.hasMore(), "CSV wire should have at least one data row");

        // Read and validate the first row of data.
        @NotNull StringBuilder row = new StringBuilder();
        wire.readEventName(row).marshallable(w -> {
            assertEquals("data1", row.toString(), "First data row should match expected key");
            wire.read(() -> "heading2").text(this, (o, s) ->
                    assertEquals("data2", s, "Heading2 value should match expected text"))
                    .read(() -> "heading3").text(this, (o, s) ->
                            assertEquals("data three", s, "Heading3 value should match expected text"));
        });
        wire.readEventName(row);
        assertTrue(wire.hasMore(), "Wire should still have another data row");

        // Read and validate the second row of data.
        wire.readEventName(row).marshallable(w -> {
            assertEquals("row2", row.toString(), "Second data row should match expected key");
            wire.read(() -> "heading2").text(this, (o, s) ->
                    assertEquals("row2b", s, "Heading2 value should match row2b text"))
                    .read(() -> "heading3").text(this, (o, s) ->
                            assertEquals("row2c", s, "Heading3 value should match row2c text"));
        });
        // Ensure no more data is present.
        wire.readEventName(row);
        assertFalse(wire.hasMore(), "Wire should be exhausted after final row");
    }

    // Test reading from another CSV formatted string.
    @Test
    @DisplayName("CSV wire parses market data rows")
    public void tstFrom2() {
        // Create a Wire object from another CSV string.
        @NotNull Wire wire = CSVWire.from(
                "Symbol,Company,Price,Change,ChangePercent,Day's Volume\n" +
                        "III,3i Group,479.4,12,2.44,2387043\n" +
                        "3IN,3i Infrastructure,164.7,0.1,0.06,429433\n" +
                        "AA,AA,325.9,5.7,1.72,1469834\n");
        // Process and validate the wire contents.
        doTestWire(wire);
        assertFalse(wire.hasMore(), "Wire should be exhausted after market data");
    }

    // Helper method to validate wire contents.
    private void doTestWire(@NotNull Wire wire) {
        // Read and validate wire contents one row at a time.
        @NotNull StringBuilder row = new StringBuilder();
        assertTrue(wire.hasMore(), "Wire should have first market data row");
        wire.readEventName(row).marshallable(w -> {
            assertEquals("III", row.toString(), "First symbol should match expected row key");
            wire.read(() -> "company").text(this, (o, s) ->
                    assertEquals("3i Group", s, "Company field should match 3i Group text"))
                    .read(() -> "price").float64(this, (o, d) ->
                            assertEquals(479.4, d, 0.0, "Price field should match 479.4 value"))
                    .read(() -> "change").float64(this, (o, d) ->
                            assertEquals(12, d, 0.0, "Change field should match 12.0 value"))
                    .read(() -> "changePercent").float64(this, (o, d) ->
                            assertEquals(2.44, d, 0.0, "Change percent should match 2.44 value"))
                    .read(() -> "daysVolume").int64(this, (o, d) ->
                            assertEquals(2387043, d, "Days volume field should match 2387043"));
        });
        wire.readEventName(row);
        assertTrue(wire.hasMore(), "Wire should have second market data row");
        wire.readEventName(row).marshallable(w -> {
            assertEquals("3IN", row.toString(), "Second symbol should match expected row key");
            wire.read(() -> "company").text(this, (o, s) ->
                    assertEquals("3i Infrastructure", s, "Company field should match 3i Infrastructure text"))
                    .read(() -> "price").float64(this, (o, d) ->
                            assertEquals(164.7, d, 0.0, "Price field should match 164.7 value"))
                    .read(() -> "change").float64(this, (o, d) ->
                            assertEquals(0.1, d, 0.0, "Change field should match 0.1 value"))
                    .read(() -> "changePercent").float64(this, (o, d) ->
                            assertEquals(0.06, d, 0.0, "Change percent should match 0.06 value"))
                    .read(() -> "daysVolume").int64(this, (o, d) ->
                            assertEquals(429433, d, "Days volume field should match 429433"))
                    .read();
        });
        wire.readEventName(row);
        assertTrue(wire.hasMore(), "Wire should have third market data row");
        wire.readEventName(row).marshallable(w -> {
            assertEquals("AA", row.toString(), "Third symbol should match expected row key");
            wire.read(() -> "company").text(this, (o, s) ->
                    assertEquals("AA", s, "Company field should match AA text"))
                    .read(() -> "price").float64(this, (o, d) ->
                            assertEquals(325.9, d, 0.0, "Price field should match 325.9 value"))
                    .read(() -> "change").float64(this, (o, d) ->
                            assertEquals(5.7, d, 0.0, "Change field should match 5.7 value"))
                    .read(() -> "changePercent").float64(this, (o, d) ->
                            assertEquals(1.72, d, 0.0, "Change percent should match 1.72 value"))
                    .read(() -> "daysVolume").int64(this, (o, d) ->
                            assertEquals(1469834, d, "Days volume field should match 1469834"));
        });
        wire.readEventName(row);
        assertFalse(wire.hasMore(), "Wire should end after third row");
    }
}
