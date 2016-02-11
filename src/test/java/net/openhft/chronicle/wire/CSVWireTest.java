/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.wire;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by peter on 27/08/15.
 */
public class CSVWireTest {

    @Test
    public void testFrom() throws Exception {
        Wire wire = CSVWire.from(
                "heading1, heading2,heading3\n" +
                        "data1, data2, \"data three\"\n" +
                        "row2, row2b, row2c\n");
        assertTrue(wire.hasMore());
        StringBuilder row = new StringBuilder();
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
        Wire wire = CSVWire.from(
                "Symbol,Company,Price,Change,ChangePercent,Day's Volume\n" +
                        "III,3i Group,479.4,12,2.44,2387043\n" +
                        "3IN,3i Infrastructure,164.7,0.1,0.06,429433\n" +
                        "AA,AA,325.9,5.7,1.72,1469834\n");
        doTestWire(wire);
    }

    @Test
    public void tstFrom3() throws IOException {
        Map<String, EndOfDayShort> map = WireType.CSV.fromFileAsMap("CSVWireTest.csv", EndOfDayShort.class);
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

    public void doTestWire(Wire wire) {
        StringBuilder row = new StringBuilder();
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