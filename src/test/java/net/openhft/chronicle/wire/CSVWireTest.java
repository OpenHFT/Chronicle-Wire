//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

// CSVWireTest class extends from WireTestCommon and tests functionality related to CSV-based wire processing.
public class CSVWireTest extends WireTestCommon {

    // Test parsing a CSV string into a wire and reading its contents.
    @Test
    public void testFrom() {
        // Create a Wire object from the given CSV string.
        @NotNull Wire wire = CSVWire.from(
                "heading1, heading2,heading3\n" +
                        "data1, data2, \"data three\"\n" +
                        "row2, row2b, row2c\n");
        // Ensure wire has content to read.
        assertTrue(wire.hasMore());

        // Read and validate the first row of data.
        @NotNull StringBuilder row = new StringBuilder();
        wire.readEventName(row).marshallable(w -> {
            assertEquals("data1", row.toString());
            wire.read(() -> "heading2").text(this, (o, s) -> assertEquals("data2", s))
                    .read(() -> "heading3").text(this, (o, s) -> assertEquals("data three", s));
        });
        wire.readEventName(row);
        assertTrue(wire.hasMore());

        // Read and validate the second row of data.
        wire.readEventName(row).marshallable(w -> {
            assertEquals("row2", row.toString());
            wire.read(() -> "heading2").text(this, (o, s) -> assertEquals("row2b", s))
                    .read(() -> "heading3").text(this, (o, s) -> assertEquals("row2c", s));
        });
        // Ensure no more data is present.
        wire.readEventName(row);
        assertFalse(wire.hasMore());
    }

    // Test reading from another CSV formatted string.
    @Test
    public void tstFrom2() {
        // Create a Wire object from another CSV string.
        @NotNull Wire wire = CSVWire.from(
                "Symbol,Company,Price,Change,ChangePercent,Day's Volume\n" +
                        "III,3i Group,479.4,12,2.44,2387043\n" +
                        "3IN,3i Infrastructure,164.7,0.1,0.06,429433\n" +
                        "AA,AA,325.9,5.7,1.72,1469834\n");
        // Process and validate the wire contents.
        doTestWire(wire);
    }

    // Helper method to validate wire contents.
    private void doTestWire(@NotNull Wire wire) {
        // Read and validate wire contents one row at a time.
        @NotNull StringBuilder row = new StringBuilder();
        assertTrue(wire.hasMore());
        wire.readEventName(row).marshallable(w -> {
            assertEquals("III", row.toString());
            wire.read(() -> "company").text("3i Group", Assert::assertEquals)
                    .read(() -> "price").float64(this, (o, d) -> assertEquals(479.4, d, 0.0))
                    .read(() -> "change").float64(this, (o, d) -> assertEquals(12, d, 0.0))
                    .read(() -> "changePercent").float64(this, (o, d) -> assertEquals(2.44, d, 0.0))
                    .read(() -> "daysVolume").int64(this, (o, d) -> assertEquals(2387043, d));
        });
        wire.readEventName(row);
        assertTrue(wire.hasMore());
        wire.readEventName(row).marshallable(w -> {
            assertEquals("3IN", row.toString());
            wire.read(() -> "company").text("3i Infrastructure", Assert::assertEquals)
                    .read(() -> "price").float64(this, (o, d) -> assertEquals(164.7, d, 0.0))
                    .read(() -> "change").float64(this, (o, d) -> assertEquals(0.1, d, 0.0))
                    .read(() -> "changePercent").float64(this, (o, d) -> assertEquals(0.06, d, 0.0))
                    .read(() -> "daysVolume").int64(this, (o, d) -> assertEquals(429433, d))
                    .read();
        });
        wire.readEventName(row);
        assertTrue(wire.hasMore());
        wire.readEventName(row).marshallable(w -> {
            assertEquals("AA", row.toString());
            wire.read(() -> "company").text("AA", Assert::assertEquals)
                    .read(() -> "price").float64(this, (o, d) -> assertEquals(325.9, d, 0.0))
                    .read(() -> "change").float64(this, (o, d) -> assertEquals(5.7, d, 0.0))
                    .read(() -> "changePercent").float64(this, (o, d) -> assertEquals(1.72, d, 0.0))
                    .read(() -> "daysVolume").int64(this, (o, d) -> assertEquals(1469834, d));
        });
        wire.readEventName(row);
        assertFalse(wire.hasMore());
    }
}
