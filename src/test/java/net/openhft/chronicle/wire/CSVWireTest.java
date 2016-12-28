/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by peter on 27/08/15.
 */
@Ignore("TODO FIX")
public class CSVWireTest {

    @Test
    public void testFrom() {
        @NotNull Wire wire = CSVWire.from(
                "heading1, heading2,heading3\n" +
                        "data1, data2, \"data three\"\n" +
                        "row2, row2b, row2c\n");
        assertTrue(wire.hasMore());
        @NotNull StringBuilder row = new StringBuilder();
        wire.readEventName(row).marshallable(w -> {
            assertEquals("data1", row.toString());
            wire.read(() -> "heading2").text(this, (o, s) -> assertEquals("data2", s))
                    .read(() -> "heading3").text(this, (o, s) -> assertEquals("data three", s));
        });
        assertTrue(wire.hasMore());
        wire.readEventName(row).marshallable(w -> {
            assertEquals("row2", row.toString());
            wire.read(() -> "heading2").text(this, (o, s) -> assertEquals("row2b", s))
                    .read(() -> "heading3").text(this, (o, s) -> assertEquals("row2c", s));
        });
        assertFalse(wire.hasMore());
    }

    @Test
    public void tstFrom2() {
        @NotNull Wire wire = CSVWire.from(
                "Symbol,Company,Price,Change,ChangePercent,Day's Volume\n" +
                        "III,3i Group,479.4,12,2.44,2387043\n" +
                        "3IN,3i Infrastructure,164.7,0.1,0.06,429433\n" +
                        "AA,AA,325.9,5.7,1.72,1469834\n");
        doTestWire(wire);
    }

    @Test
    public void tstFrom3() throws IOException {
        @NotNull Map<String, EndOfDayShort> map = WireType.CSV.fromFileAsMap("CSVWireTest.csv", EndOfDayShort.class);
        assertEquals("{III=!net.openhft.chronicle.wire.EndOfDayShort {\n" +
                "  name: \"3i Group\",\n" +
                "  price: 479.4,\n" +
                "  change: 12.0,\n" +
                "  changePercent: 2.44,\n" +
                "  daysVolume: 2387043\n" +
                "}\n" +
                ", 3IN=!net.openhft.chronicle.wire.EndOfDayShort {\n" +
                "  name: \"3i Infrastructure\",\n" +
                "  price: 164.7,\n" +
                "  change: 0.1,\n" +
                "  changePercent: 0.06,\n" +
                "  daysVolume: 429433\n" +
                "}\n" +
                ", AA=!net.openhft.chronicle.wire.EndOfDayShort {\n" +
                "  name: AA,\n" +
                "  price: 325.9,\n" +
                "  change: 5.7,\n" +
                "  changePercent: 1.72,\n" +
                "  daysVolume: 1469834\n" +
                "}\n" +
                "}", map.toString());
    }

    public void doTestWire(@NotNull Wire wire) {
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
        assertTrue(wire.hasMore());
        wire.readEventName(row).marshallable(w -> {
            assertEquals("3IN", row.toString());
            wire.read(() -> "company").text("3i Infrastructure", Assert::assertEquals)
                    .read(() -> "price").float64(this, (o, d) -> assertEquals(164.7, d, 0.0))
                    .read(() -> "change").float64(this, (o, d) -> assertEquals(0.1, d, 0.0))
                    .read(() -> "changePercent").float64(this, (o, d) -> assertEquals(0.06, d, 0.0))
                    .read(() -> "daysVolume").int64(this, (o, d) -> assertEquals(429433, d));
        });
        assertTrue(wire.hasMore());
        wire.readEventName(row).marshallable(w -> {
            assertEquals("AA", row.toString());
            wire.read(() -> "company").text("AA", Assert::assertEquals)
                    .read(() -> "price").float64(this, (o, d) -> assertEquals(325.9, d, 0.0))
                    .read(() -> "change").float64(this, (o, d) -> assertEquals(5.7, d, 0.0))
                    .read(() -> "changePercent").float64(this, (o, d) -> assertEquals(1.72, d, 0.0))
                    .read(() -> "daysVolume").int64(this, (o, d) -> assertEquals(1469834, d));
        });
        assertFalse(wire.hasMore());
    }
}