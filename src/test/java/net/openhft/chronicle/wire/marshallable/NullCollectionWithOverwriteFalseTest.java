/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;

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
     * Explicit null replaces an inline-initialised field when overwrite is enabled.
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
     * Explicit null replaces every container default when overwrite is enabled.
     */
    @Test
    public void nullContainersWithOverwrite() throws Exception {
        checkContainers(true, true);
    }

    /**
     * Explicit null restores null, empty or populated defaults when overwrite is disabled.
     */
    @Test
    public void nullContainersWithoutOverwrite() throws Exception {
        checkContainers(false, true);
    }

    /**
     * Present containers replace stale contents in both modes, including on destination reuse.
     */
    @Test
    public void emptyAndPopulatedContainersReplacePreviousContents() throws Exception {
        checkContainers(false, false);
        checkContainers(true, false);
    }

    /**
     * Missing fields restore defaults with overwrite enabled and retain existing contents otherwise.
     */
    @Test
    public void absentContainersRespectOverwrite() throws Exception {
        for (WireType type : new WireType[]{WireType.TEXT, WireType.BINARY}) {
            for (boolean overwrite : new boolean[]{false, true}) {
                Containers target = new Containers();
                target.strings.add("stale");
                target.numbers.add(99);
                target.states.add(Thread.State.TERMINATED);
                target.map.put("stale", 99);
                Bytes<?> bytes = Bytes.allocateElasticOnHeap();
                try {
                    Wires.readMarshallable(target, type.apply(bytes), overwrite);
                    Containers defaults = new Containers();
                    if (!overwrite) {
                        defaults.strings.add("stale");
                        defaults.numbers.add(99);
                        defaults.states.add(Thread.State.TERMINATED);
                        defaults.map.put("stale", 99);
                    }
                    for (Field field : Containers.class.getDeclaredFields())
                        assertEquals(type + " absent " + field.getName(), field.get(defaults), field.get(target));
                } finally {
                    bytes.releaseLast();
                }
            }
        }
    }

    private void checkContainers(boolean overwrite, boolean nullInput) throws Exception {
        for (WireType type : new WireType[]{WireType.TEXT, WireType.BINARY}) {
            Containers source = new Containers();
            Containers target = new Containers();
            Containers defaults = new Containers();
            for (Field field : Containers.class.getDeclaredFields()) {
                if (nullInput)
                    field.set(source, null);
            }
            target.emptyStrings.add("stale");
            target.strings.add("stale");
            target.emptyNumbers.add(99);
            target.numbers.add(99);
            target.emptyStates.add(Thread.State.TERMINATED);
            target.states.add(Thread.State.TERMINATED);
            target.emptyMap.put("stale", 99);
            target.map.put("stale", 99);
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            try {
                Wire wire = type.apply(bytes);
                source.writeMarshallable(wire);
                Wires.readMarshallable(target, wire, overwrite);
                for (Field field : Containers.class.getDeclaredFields()) {
                    Object expected = nullInput && overwrite ? null : field.get(defaults);
                    assertEquals(type + " " + field.getName(), expected, field.get(target));
                }
                // Reuse the destination after a read that may have cleared its fields.
                bytes.clear();
                defaults.writeMarshallable(wire);
                Wires.readMarshallable(target, wire, overwrite);
                for (Field field : Containers.class.getDeclaredFields())
                    assertEquals(type + " reused " + field.getName(), field.get(defaults), field.get(target));
            } finally {
                bytes.releaseLast();
            }
        }
    }

    /**
     * Null, empty and populated defaults exercise the String collection, general collection,
     * EnumSet and Map readers. Tests use separate input, destination and default instances
     * with text and binary wires; Thread.State supplies enum values only.
     */
    static class Containers extends SelfDescribingMarshallable {
        List<String> nullStrings;
        List<String> emptyStrings = new ArrayList<>();
        List<String> strings = new ArrayList<>(Collections.singletonList("default"));
        List<Integer> nullNumbers;
        List<Integer> emptyNumbers = new ArrayList<>();
        List<Integer> numbers = new ArrayList<>(Collections.singletonList(1));
        EnumSet<Thread.State> nullStates;
        EnumSet<Thread.State> emptyStates = EnumSet.noneOf(Thread.State.class);
        EnumSet<Thread.State> states = EnumSet.of(Thread.State.NEW);
        Map<String, Integer> nullMap;
        Map<String, Integer> emptyMap = new LinkedHashMap<>();
        Map<String, Integer> map = new LinkedHashMap<>(Collections.singletonMap("default", 1));
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
