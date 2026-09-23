/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Pins the contract for String collection, general collection, EnumSet and Map fields read with
 * {@link Wires#readMarshallable(Object, WireIn, boolean)}.
 *
 * <pre>
 * Input field           | overwrite=false                  | overwrite=true
 * ----------------------+----------------------------------+-----------------------------
 * absent                | keep the destination's value     | restore the declared default
 * present and null      | restore the declared default     | set null
 * present and empty     | set empty                        | set empty
 * present and populated | replace with the input           | replace with the input
 * </pre>
 * <p>
 * Expected values come from {@link #expected}, never from a second read or from the DTO's own writer:
 * text input is a literal and binary input is built with the low level {@link ValueOut} calls.
 *
 * @see NullCollectionWithOverwriteFalseTest
 */
@RunWith(value = Parameterized.class)
public class ContainerOverwriteContractTest extends WireTestCommon {

    private static final int MARKER = 7;
    private static final WireType[] WIRE_TYPES = {WireType.TEXT, WireType.BINARY, WireType.BINARY_LIGHT};

    private final WireType wireType;
    private final boolean unexpectedFields;
    private final Kind kind;

    public ContainerOverwriteContractTest(WireType wireType, boolean unexpectedFields, Kind kind) {
        this.wireType = wireType;
        this.unexpectedFields = unexpectedFields;
        this.kind = kind;
    }

    /**
     * A DTO that overrides {@code unexpectedField}, as every {@link AbstractMarshallableCfg} does, is read by
     * a different loop which never visits absent fields, so both are covered.
     */
    @Parameterized.Parameters(name = "{0} unexpectedFields={1} {2}")
    public static Collection<Object[]> combinations() {
        List<Object[]> list = new ArrayList<>();
        for (WireType wireType : WIRE_TYPES)
            for (boolean unexpectedFields : new boolean[]{false, true})
                for (Kind kind : Kind.values())
                    list.add(new Object[]{wireType, unexpectedFields, kind});
        return list;
    }

    /**
     * The one place the contract is written down.
     *
     * @param before          the destination's value before the read
     * @param declaredDefault the value the field has after the no-argument constructor
     * @param inputValue      the value carried by the input when it is present and not null
     */
    static Object expected(Input input, boolean overwrite, Object before, Object declaredDefault, Object inputValue) {
        switch (input) {
            case ABSENT:
                return overwrite ? declaredDefault : before;
            case NULL:
                return overwrite ? null : declaredDefault;
            default:
                return inputValue;
        }
    }

    /**
     * Guards this test's own premise rather than a production change: without it both values of
     * {@code unexpectedFields} could silently exercise the same read loop.
     */
    @Test
    public void usesTheIntendedReadLoop() {
        WireMarshaller<?> marshaller = WireMarshaller.WIRE_MARSHALLER_CL.get(newContainers().getClass());
        assertEquals(unexpectedFields, marshaller instanceof WireMarshallerForUnexpectedFields);
    }

    /**
     * Every input state against a fresh destination, one holding stale entries and one whose field was nulled.
     */
    @Test
    public void singleRead() throws Exception {
        List<String> failures = new ArrayList<>();
        for (Default dflt : Default.values())
            for (boolean overwrite : new boolean[]{false, true})
                for (Destination destination : Destination.values())
                    for (Input input : Input.values()) {
                        Containers target = newContainers();
                        Field field = field(dflt);
                        if (destination == Destination.STALE)
                            field.set(target, kind.stale());
                        else if (destination == Destination.NULLED)
                            field.set(target, null);
                        Object before = kind.copy(field.get(target));

                        read(target, field.getName(), input, kind.values1(), overwrite);

                        Object expected = expected(input, overwrite, before, declaredDefault(dflt), kind.value(input, kind.values1()));
                        check(failures, "default=" + dflt + " overwrite=" + overwrite + " dest=" + destination + " input=" + input,
                                expected, field.get(target), target);
                    }
        assertTrue(failures.size() + " cells failed:\n" + String.join("\n", failures), failures.isEmpty());
    }

    /**
     * Every ordered pair of inputs read into one destination that starts fresh; nothing from the first read may
     * survive unless the contract says so. Destinations that start stale or nulled are covered by {@link #singleRead()}.
     */
    @Test
    public void reusedDestination() throws Exception {
        List<String> failures = new ArrayList<>();
        for (Default dflt : Default.values())
            for (boolean overwrite : new boolean[]{false, true})
                for (Input first : Input.values())
                    for (Input second : Input.values()) {
                        Containers target = newContainers();
                        Field field = field(dflt);
                        Object declaredDefault = declaredDefault(dflt);

                        read(target, field.getName(), first, kind.values1(), overwrite);
                        Object afterFirst = expected(first, overwrite, declaredDefault, declaredDefault, kind.value(first, kind.values1()));
                        String cell = "default=" + dflt + " overwrite=" + overwrite + " first=" + first;
                        check(failures, cell, afterFirst, field.get(target), target);

                        // after a failed first read, continue from the contractual state so it is reported once
                        if (!Objects.equals(afterFirst, field.get(target)))
                            field.set(target, kind.copy(afterFirst));
                        read(target, field.getName(), second, kind.values2(), overwrite);
                        Object afterSecond = expected(second, overwrite, afterFirst, declaredDefault, kind.value(second, kind.values2()));
                        check(failures, cell + " second=" + second, afterSecond, field.get(target), target);
                    }
        assertTrue(failures.size() + " cells failed:\n" + String.join("\n", failures), failures.isEmpty());
    }

    /**
     * A restored container is a separate instance, so adding to it must not leak into the marshaller's defaults or into
     * another destination. Elements are copied by reference; this does not ask for a deep copy.
     */
    @Test
    public void restoredContainerIsNotShared() throws Exception {
        for (Default dflt : new Default[]{Default.EMPTY, Default.POPULATED}) {
            Field field = field(dflt);
            Object declaredDefault = declaredDefault(dflt);
            // the two ways a default is restored: explicit null without overwrite, and absent with overwrite
            Object[][] restores = {{Input.NULL, false}, {Input.ABSENT, true}};
            for (Object[] restore : restores) {
                Input input = (Input) restore[0];
                boolean overwrite = (Boolean) restore[1];
                String cell = "default=" + dflt + " input=" + input + " overwrite=" + overwrite;
                for (Destination destination : new Destination[]{Destination.STALE, Destination.NULLED}) {
                    Containers first = newContainers();
                    field.set(first, destination == Destination.STALE ? kind.stale() : null);
                    read(first, field.getName(), input, kind.values1(), overwrite);
                    assertEquals(cell, declaredDefault, field.get(first));

                    Object marshallerDefault = field.get(WireMarshaller.WIRE_MARSHALLER_CL.get(first.getClass()).defaultValue());
                    assertNotSame(cell + " dest=" + destination, marshallerDefault, field.get(first));
                    kind.mutate(field.get(first));
                    assertNotEquals(cell, declaredDefault, field.get(first));

                    assertEquals(cell + " marshaller default changed", declaredDefault, marshallerDefault);
                    Containers second = newContainers();
                    field.set(second, destination == Destination.STALE ? kind.stale() : null);
                    read(second, field.getName(), input, kind.values1(), overwrite);
                    assertEquals(cell + " second destination", declaredDefault, field.get(second));
                    assertNotSame(cell, field.get(first), field.get(second));
                }
            }
        }
    }

    private Containers newContainers() {
        return unexpectedFields ? new ContainersWithUnexpectedFields() : new Containers();
    }

    private Field field(Default dflt) throws NoSuchFieldException {
        Field field = Containers.class.getDeclaredField(kind.prefix + dflt.suffix);
        field.setAccessible(true);
        return field;
    }

    private Object declaredDefault(Default dflt) throws Exception {
        return field(dflt).get(newContainers());
    }

    private void check(List<String> failures, String cell, Object expected, Object actual, Containers target) {
        if (!Objects.equals(expected, actual))
            failures.add(cellPrefix() + cell + ": expected=" + expected + " actual=" + actual);
        // the field after the container must still be readable, i.e. the container was consumed exactly
        if (target.marker != MARKER)
            failures.add(cellPrefix() + cell + ": marker=" + target.marker);
        target.marker = 0;
    }

    private String cellPrefix() {
        return wireType + " unexpectedFields=" + unexpectedFields + " " + kind + " ";
    }

    private void read(Containers target, String name, Input input, List<?> values, boolean overwrite) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            Wire wire = wireType.apply(bytes);
            if (wireType == WireType.TEXT) {
                bytes.append(kind.text(name, input, values)).append("marker: " + MARKER + "\n");
            } else {
                kind.write(wire, name, input, values);
                wire.write("marker").int32(MARKER);
            }
            Wires.readMarshallable(target, wire, overwrite);
        } finally {
            bytes.releaseLast();
        }
    }

    enum Input {ABSENT, NULL, EMPTY, POPULATED}

    enum Destination {FRESH, STALE, NULLED}

    enum Default {
        NULL("Null"), EMPTY("Empty"), POPULATED("Populated");

        final String suffix;

        Default(String suffix) {
            this.suffix = suffix;
        }
    }

    enum Kind {
        STRING_LIST("stringList", Arrays.asList("in1", "in2"), Collections.singletonList("in3"), "stale"),
        STRING_SET("stringSet", Arrays.asList("in1", "in2"), Collections.singletonList("in3"), "stale"),
        INTEGER_LIST("integerList", Arrays.asList(11, 12), Collections.singletonList(13), 99),
        ENUM_SET("enumSet", Arrays.asList(Thread.State.RUNNABLE, Thread.State.BLOCKED),
                Collections.singletonList(Thread.State.TIMED_WAITING), Thread.State.TERMINATED),
        MAP("map", Arrays.asList("in1", "in2"), Collections.singletonList("in3"), "stale");

        final String prefix;
        private final List<?> values1;
        private final List<?> values2;
        private final Object staleValue;

        Kind(String prefix, List<?> values1, List<?> values2, Object staleValue) {
            this.prefix = prefix;
            this.values1 = values1;
            this.values2 = values2;
            this.staleValue = staleValue;
        }

        List<?> values1() {
            return values1;
        }

        List<?> values2() {
            return values2;
        }

        /**
         * @return a new mutable container of this kind holding {@code values}; map values are the key's last digit
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object of(List<?> values) {
            switch (this) {
                case STRING_LIST:
                case INTEGER_LIST:
                    return new ArrayList<>(values);
                case STRING_SET:
                    return new LinkedHashSet<>(values);
                case ENUM_SET:
                    EnumSet<Thread.State> set = EnumSet.noneOf(Thread.State.class);
                    set.addAll((List) values);
                    return set;
                default:
                    Map<String, Integer> map = new LinkedHashMap<>();
                    for (Object key : values)
                        map.put((String) key, mapValue(key));
                    return map;
            }
        }

        static int mapValue(Object key) {
            String s = key.toString();
            return Character.isDigit(s.charAt(s.length() - 1)) ? s.charAt(s.length() - 1) - '0' : 99;
        }

        Object stale() {
            return of(Collections.singletonList(staleValue));
        }

        Object value(Input input, List<?> values) {
            return input == Input.EMPTY ? of(Collections.emptyList()) : input == Input.POPULATED ? of(values) : null;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        Object copy(Object container) {
            if (container == null)
                return null;
            if (this == MAP)
                return new LinkedHashMap<>((Map) container);
            return of(new ArrayList<>((Collection) container));
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        void mutate(Object container) {
            if (this == MAP)
                ((Map) container).put("mutated", 1);
            else
                ((Collection) container).add(this == INTEGER_LIST ? (Object) 1234 : this == ENUM_SET ? Thread.State.TERMINATED : "mutated");
        }

        /**
         * @return literal text for one field, written by hand rather than by the DTO
         */
        String text(String name, Input input, List<?> values) {
            switch (input) {
                case ABSENT:
                    return "";
                case NULL:
                    return name + ": !!null \"\"\n";
                case EMPTY:
                    return name + (this == MAP ? ": { }\n" : ": [ ]\n");
                default:
                    StringJoiner joiner = this == MAP ? new StringJoiner(", ", ": { ", " }\n") : new StringJoiner(", ", ": [ ", " ]\n");
                    for (Object value : values)
                        joiner.add(this == MAP ? value + ": " + mapValue(value) : value.toString());
                    return name + joiner;
            }
        }

        /**
         * Writes one field with the low level API so the DTO's own writer is not involved.
         */
        void write(Wire wire, String name, Input input, List<?> values) {
            if (input == Input.ABSENT)
                return;
            ValueOut out = wire.write(name);
            List<?> items = input == Input.EMPTY ? Collections.emptyList() : values;
            if (input == Input.NULL)
                out.nu11();
            else if (this == MAP)
                out.marshallable(w -> {
                    for (Object key : items)
                        w.write(key.toString()).int32(mapValue(key));
                });
            else
                out.sequence(items, (list, o) -> {
                    for (Object item : list)
                        if (item instanceof Integer)
                            o.int32((Integer) item);
                        else
                            o.text(item.toString());
                });
        }
    }

    /**
     * A null, an empty and a populated default for each container reader, followed by a marker.
     */
    static class Containers extends SelfDescribingMarshallable {
        List<String> stringListNull;
        List<String> stringListEmpty = new ArrayList<>();
        List<String> stringListPopulated = new ArrayList<>(Arrays.asList("default1", "default2"));
        Set<String> stringSetNull;
        Set<String> stringSetEmpty = new LinkedHashSet<>();
        Set<String> stringSetPopulated = new LinkedHashSet<>(Arrays.asList("default1", "default2"));
        List<Integer> integerListNull;
        List<Integer> integerListEmpty = new ArrayList<>();
        List<Integer> integerListPopulated = new ArrayList<>(Arrays.asList(1, 2));
        EnumSet<Thread.State> enumSetNull;
        EnumSet<Thread.State> enumSetEmpty = EnumSet.noneOf(Thread.State.class);
        EnumSet<Thread.State> enumSetPopulated = EnumSet.of(Thread.State.NEW, Thread.State.WAITING);
        Map<String, Integer> mapNull;
        Map<String, Integer> mapEmpty = new LinkedHashMap<>();
        Map<String, Integer> mapPopulated = new LinkedHashMap<>(Collections.singletonMap("default1", 1));
        int marker;
    }

    /**
     * Selects {@code WireMarshallerForUnexpectedFields}.
     */
    static class ContainersWithUnexpectedFields extends Containers {
        @Override
        public void unexpectedField(Object event, ValueIn valueIn) {
            throw new AssertionError("unexpected field " + event);
        }
    }
}
