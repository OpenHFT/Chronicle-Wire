/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
public class JSONWireMiscTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Constant representing the text value for tests
    private static final String TEXT = "abc";

    // Flag to indicate if types should be used or not
    private boolean useTypes;

    // Instance of JSONWire which will be used in the tests
    private JSONWire wire;

    // Constructor to initialize the parameterized test instance with useTypes value
    public void initJSONWireMiscTest(boolean useTypes) {
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
    public void before() {
        wire = new JSONWire().useTypes(useTypes);
    }

    // Test to write a byte array to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0}")
    public void bytesByteArray(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        final byte[] arr = TEXT.getBytes(StandardCharsets.UTF_8);
        wire.getValueOut().bytes(arr);
        final String actual = wire.toString();
        assertBalancedBrackets(actual);
        System.out.println("actual = " + actual);
    }

    // Test to write a byte array with a given name to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0}")
    public void bytesStringByteArray(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        final byte[] arr = TEXT.getBytes(StandardCharsets.UTF_8);
        wire.getValueOut().bytes("binary", arr);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a Bytes object with a given name to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0}")
    public void bytesStringBytes(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        final Bytes<?> bytes = Bytes.from(TEXT);
        wire.getValueOut().bytes("binary", bytes);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a sequence of strings to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0}")
    public void sequenceOfStrings(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        final List<String> list = Arrays.asList("A", "B", "C");
        wire.getValueOut().sequence(list);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write an enum value to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0}")
    public void asEnum(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        wire.getValueOut().asEnum(A.SECOND);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a sequence of enums to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0}")
    public void sequenceOfEnums(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        final List<A> list = Arrays.asList(A.values());
        wire.getValueOut().sequence(list);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a set of enums to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0}")
    public void sequenceOfSet(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        final Set<A> set = EnumSet.allOf(A.class);
        wire.getValueOut().sequence(set);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a sorted set of enums to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0}")
    public void sequenceOfSortedSet(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        final Set<A> set = EnumSet.allOf(A.class);
        wire.getValueOut().sequence(set);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a LocalTime instance to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0}")
    public void localTime(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        final LocalTime localTime = LocalTime.parse("17:01");
        wire.getValueOut().object(localTime);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a sequence of custom class Foo instances to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0}")
    public void sequenceOfCustomClass(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        final List<Foo> list = Arrays.asList(new Foo(0), new Foo(1), new Foo(2));
        wire.getValueOut().sequence(list);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a custom class Bar instance to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0}")
    public void customClass(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        wire.getValueOut().object(new Bar("Bazz"));
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a Duration instance to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0}")
    public void duration(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        final Duration duration = Duration.ofSeconds(63);
        wire.getValueOut().object(duration);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a serializable class instance to the wire and verify the written content
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0}")
    public void serializable(boolean useTypes) {
        initJSONWireMiscTest(useTypes);
        assumeFalse(Jvm.maxDirectMemory() == 0);

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
    static final class Foo {
        final int value;

        Foo(int value) {
            this.value = value;
        }
    }

    // Custom class definition with a string value for testing
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
