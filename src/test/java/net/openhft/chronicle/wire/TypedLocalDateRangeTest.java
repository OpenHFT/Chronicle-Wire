/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import static org.junit.Assert.*;

import net.openhft.chronicle.bytes.*;

import org.junit.Test;

import java.io.Serializable;
import java.lang.annotation.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

public class TypedLocalDateRangeTest extends WireTestCommon {

    private void checkDate(int year, int month, int day) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            Wire wire = new BinaryWire(bytes);
            LocalDate date = LocalDate.of(year, month, day);
            wire.write("date").typedMarshallable((Serializable) date);
            assertEquals(date, wire.read("date").object(LocalDate.class));
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void ordinaryYearRoundTrips() throws Exception {
        checkDate(2026, 11, 16);
    }

    @Test
    public void shortYearRoundTrips() throws Exception {
        checkDate(99, 2, 3);
    }

    @Test
    public void yearZeroRoundTrips() throws Exception {
        checkDate(0, 2, 29);
    }

    @Test
    public void negativeYearRoundTrips() throws Exception {
        checkDate(-1, 2, 3);
    }

    @Test
    public void expandedYearRoundTrips() throws Exception {
        checkDate(10000, 2, 3);
    }
}
