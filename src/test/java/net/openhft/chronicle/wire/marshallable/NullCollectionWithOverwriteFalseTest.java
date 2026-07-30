/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

/**
 * Test class to validate that a null collection survives a round trip when overwriting is disabled.
 *
 * @see MarshallableWithOverwriteFalseTest
 */
public class NullCollectionWithOverwriteFalseTest extends WireTestCommon {

    /**
     * A null {@code Set<String>} remains null after a round trip.
     */
    @Test
    public void nullStringSetSurvivesRoundTrip() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        MyStringSetDto dto = new MyStringSetDto();
        assertNull(dto.strings);

        // The field must be written as present-and-null; an absent field never reaches readValue
        String cs = dto.toString();
        assertTrue(cs, cs.contains("!!null"));

        MyStringSetDto o = Marshallable.fromString(MyStringSetDto.class, cs);
        assertNull(o.strings);
    }

    /**
     * A null {@code List<String>} remains null after a round trip.
     */
    @Test
    public void nullStringListSurvivesRoundTrip() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        MyStringListDto dto = new MyStringListDto();
        assertNull(dto.strings);

        String cs = dto.toString();
        assertTrue(cs, cs.contains("!!null"));

        MyStringListDto o = Marshallable.fromString(MyStringListDto.class, cs);
        assertNull(o.strings);
    }

    /**
     * A field initialised inline resolves to empty rather than null, as before.
     */
    @Test
    public void inlineInitialisedListResolvesToEmpty() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        MyInitialisedListDto dto = new MyInitialisedListDto();
        dto.strings = null;

        String cs = dto.toString();
        assertTrue(cs, cs.contains("!!null"));

        MyInitialisedListDto o = Marshallable.fromString(MyInitialisedListDto.class, cs);
        assertNotNull(o.strings);
        assertEquals(0, o.strings.size());
    }

    /**
     * A non-String component type routes to CollectionFieldAccess and is expected to pass already.
     */
    @Test
    public void nullIntegerSetSurvivesRoundTrip() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        MyIntegerSetDto dto = new MyIntegerSetDto();
        assertNull(dto.numbers);

        String cs = dto.toString();
        assertTrue(cs, cs.contains("!!null"));

        MyIntegerSetDto o = Marshallable.fromString(MyIntegerSetDto.class, cs);
        assertNull(o.numbers);
    }

    /**
     * Reading with overwrite enabled is expected to pass already.
     */
    @Test
    public void nullStringSetSurvivesRoundTripWithOverwrite() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        MyOverwriteTrueDto dto = new MyOverwriteTrueDto();
        assertNull(dto.strings);

        String cs = dto.toString();
        assertTrue(cs, cs.contains("!!null"));

        MyOverwriteTrueDto o = Marshallable.fromString(MyOverwriteTrueDto.class, cs);
        assertNull(o.strings);
    }

    /**
     * Pins the current behaviour of an inline-initialised field read with overwrite enabled,
     * which no other test states. Mirroring CollectionFieldAccess would resolve this to empty.
     */
    @Test
    public void inlineInitialisedListWithOverwrite() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        MyInitialisedListOverwriteTrueDto dto = new MyInitialisedListOverwriteTrueDto();
        dto.strings = null;

        String cs = dto.toString();
        assertTrue(cs, cs.contains("!!null"));

        MyInitialisedListOverwriteTrueDto o = Marshallable.fromString(MyInitialisedListOverwriteTrueDto.class, cs);
        assertNull(o.strings);
    }

    /**
     * Inner class with an uninitialised {@code Set<String>} and overwrite disabled.
     */
    static class MyStringSetDto extends SelfDescribingMarshallable {
        Set<String> strings;

        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            Wires.readMarshallable(this, wire, false);
        }
    }

    /**
     * Inner class with an uninitialised {@code List<String>} and overwrite disabled.
     */
    static class MyStringListDto extends SelfDescribingMarshallable {
        List<String> strings;

        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            Wires.readMarshallable(this, wire, false);
        }
    }

    /**
     * Inner class whose list is initialised inline, as in {@link MarshallableWithOverwriteFalseTest}.
     */
    static class MyInitialisedListDto extends SelfDescribingMarshallable {
        List<String> strings = new ArrayList<>();

        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            Wires.readMarshallable(this, wire, false);
        }
    }

    /**
     * Inner class with an uninitialised {@code Set<Integer>} and overwrite disabled.
     */
    static class MyIntegerSetDto extends SelfDescribingMarshallable {
        Set<Integer> numbers;

        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            Wires.readMarshallable(this, wire, false);
        }
    }

    /**
     * Inner class with an uninitialised {@code Set<String>} and overwrite enabled.
     */
    static class MyOverwriteTrueDto extends SelfDescribingMarshallable {
        Set<String> strings;

        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            Wires.readMarshallable(this, wire, true);
        }
    }

    /**
     * Inner class whose list is initialised inline, read with overwrite enabled.
     */
    static class MyInitialisedListOverwriteTrueDto extends SelfDescribingMarshallable {
        List<String> strings = new ArrayList<>();

        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            Wires.readMarshallable(this, wire, true);
        }
    }
}
