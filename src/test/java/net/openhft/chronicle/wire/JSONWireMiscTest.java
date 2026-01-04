/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

import static net.openhft.chronicle.wire.JsonUtil.assertBalancedBrackets;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * relates to https://github.com/OpenHFT/Chronicle-Wire/issues/324
 */
@SuppressWarnings({"deprecation", "removal"})
class JSONWireMiscTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Constant representing the text value for tests
    private static final String TEXT = "abc";

    // Flag to indicate if types should be used or not
    private boolean useTypes;

    // Instance of JSONWire which will be used in the tests
    private JSONWire wire;

    // Constructor to initialize the parameterized test instance with useTypes value
    void initJSONWireMiscTest(boolean useTypes) {
        this.useTypes = useTypes;
    }

    // Parameterized test data provider
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{true},
                new Object[]{false}
        );
    }

    // Setup method to initialize JSONWire with or without types based on the test instance
    @BeforeEach
    void before() {
        wire = new JSONWire().useTypes(useTypes);
    }

    // Test to write a byte array to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} writes json byte array output")
    @DisplayName("Writes byte array values to json wire")
    void bytesByteArray(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        final byte[] arr = TEXT.getBytes(StandardCharsets.UTF_8);
        wire.getValueOut().bytes(arr);
        final String actual = wire.toString();
        assertBalancedBrackets(actual);
        System.out.println("actual = " + actual);
    }

    // Test to write a byte array with a given name to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} writes named json byte array output")
    @DisplayName("Writes named byte array values to json wire")
    void bytesStringByteArray(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        final byte[] arr = TEXT.getBytes(StandardCharsets.UTF_8);
        wire.getValueOut().bytes("binary", arr);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a Bytes object with a given name to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} writes named json bytes store output")
    @DisplayName("Writes named bytes store values to json wire")
    void bytesStringBytes(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        final Bytes<?> bytes = Bytes.from(TEXT);
        wire.getValueOut().bytes("binary", bytes);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a sequence of strings to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} writes json sequence of strings")
    @DisplayName("Writes sequence of strings to json wire")
    void sequenceOfStrings(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        final List<String> list = Arrays.asList("A", "B", "C");
        wire.getValueOut().sequence(list);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write an enum value to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} writes json enum value output")
    @DisplayName("Writes enum value to json wire")
    void asEnum(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        wire.getValueOut().asEnum(A.SECOND);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a sequence of enums to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} writes json sequence of enums")
    @DisplayName("Writes sequence of enums to json wire")
    void sequenceOfEnums(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        final List<A> list = Arrays.asList(A.values());
        wire.getValueOut().sequence(list);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a set of enums to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} writes json enum set values")
    @DisplayName("Writes enum set values to json wire")
    void sequenceOfSet(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        final Set<A> set = EnumSet.allOf(A.class);
        wire.getValueOut().sequence(set);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a sorted set of enums to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} writes json sorted enum set")
    @DisplayName("Writes sorted enum set to json wire")
    void sequenceOfSortedSet(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        final Set<A> set = EnumSet.allOf(A.class);
        wire.getValueOut().sequence(set);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a LocalTime instance to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} writes json LocalTime value output")
    @DisplayName("Writes local time value to json wire")
    void localTime(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        final LocalTime localTime = LocalTime.parse("17:01");
        wire.getValueOut().object(localTime);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a sequence of custom class Foo instances to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} writes json sequence of Foo values")
    @DisplayName("Writes custom class sequence to json wire")
    void sequenceOfCustomClass(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        final List<Foo> list = Arrays.asList(new Foo(0), new Foo(1), new Foo(2));
        wire.getValueOut().sequence(list);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a custom class Bar instance to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} writes json Bar object output")
    @DisplayName("Writes custom class instance to json wire")
    void customClass(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        wire.getValueOut().object(new Bar("Bazz"));
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a Duration instance to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} writes json Duration value output")
    @DisplayName("Writes duration value to json wire")
    void duration(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        final Duration duration = Duration.ofSeconds(63);
        wire.getValueOut().object(duration);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a serializable class instance to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} writes json serialisable object output")
    @DisplayName("Writes serialisable object to json wire")
    void serializable(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory disabled; skip json serialisable test");

        final Ser s = new Ser();
        wire.getValueOut().object(s);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Enum definitions to be used for the asEnum test
    enum A {
        FIRST, SECOND, THIRD
    }

    // Custom class definition with an integer value for testing
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static final class Foo {
        final int value;

        Foo(int value) {
            this.value = value;
        }
    }

    // Custom class definition with a string value for testing
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static final class Bar {

        final String value;

        Bar(String value) {
            this.value = value;
        }
    }

    // Custom class implementing Serializable interface for testing
    static final class Ser implements Serializable {
        private static final long serialVersionUID = 0L;
        int foo;
    }
}
