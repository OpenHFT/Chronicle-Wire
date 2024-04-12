/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

import static net.openhft.chronicle.wire.JsonUtil.assertBalancedBrackets;

/**
 * relates to https://github.com/OpenHFT/Chronicle-Wire/issues/324
 */
@RunWith(value = Parameterized.class)
public class JSONWireMiscTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Constant representing the text value for tests
    private final String TEXT = "abc";

    // Flag to indicate if types should be used or not
    private final boolean useTypes;

    // Instance of JSONWire which will be used in the tests
    private JSONWire wire;

    // Parameterized test data provider
    @Parameterized.Parameters(name = "useTypes={0}")
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{true},
                new Object[]{false}
        );
    }

    // Constructor to initialize the parameterized test instance with useTypes value
    public JSONWireMiscTest(boolean useTypes) {
        this.useTypes = useTypes;
    }

    // Setup method to initialize JSONWire with or without types based on the test instance
    @Before
    public void before() {
        wire = new JSONWire().useTypes(useTypes);
    }

    // Test to write a byte array to the wire and verify the written content
    @Test
    public void bytesByteArray() {
        final byte[] arr = TEXT.getBytes(StandardCharsets.UTF_8);
        wire.getValueOut().bytes(arr);
        final String actual = wire.toString();
        assertBalancedBrackets(actual);
        System.out.println("actual = " + actual);
    }

    // Test to write a byte array with a given name to the wire and verify the written content
    @Test
    public void bytesStringByteArray() {
        final byte[] arr = TEXT.getBytes(StandardCharsets.UTF_8);
        wire.getValueOut().bytes("binary", arr);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a Bytes object with a given name to the wire and verify the written content
    @Test
    public void bytesStringBytes() {
        final Bytes<?> bytes = Bytes.from(TEXT);
        wire.getValueOut().bytes("binary", bytes);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a sequence of strings to the wire and verify the written content
    @Test
    public void sequenceOfStrings() {
        final List<String> list = Arrays.asList("A", "B", "C");
        wire.getValueOut().sequence(list);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Enum definitions to be used for the asEnum test
    enum A {
        FIRST, SECOND, THIRD;
    }

    // Test to write an enum value to the wire and verify the written content
    @Test
    public void asEnum() {
        wire.getValueOut().asEnum(A.SECOND);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a sequence of enums to the wire and verify the written content
    @Test
    public void sequenceOfEnums() {
        final List<A> list = Arrays.asList(A.values());
        wire.getValueOut().sequence(list);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a set of enums to the wire and verify the written content
    @Test
    public void sequenceOfSet() {
        final Set<A> set = new HashSet<>(Arrays.asList(A.values()));
        wire.getValueOut().sequence(set);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a sorted set of enums to the wire and verify the written content
    @Test
    public void sequenceOfSortedSet() {
        final Set<A> set = new TreeSet<>(Arrays.asList(A.values()));
        wire.getValueOut().sequence(set);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a LocalTime instance to the wire and verify the written content
    @Test
    public void localTime() {
        final LocalTime localTime = LocalTime.parse("17:01");
        wire.getValueOut().object(localTime);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Custom class definition with an integer value for testing
    final static class Foo {
        final int value;

        public Foo(int value) {
            this.value = value;
        }
    }

    // Test to write a sequence of custom class Foo instances to the wire and verify the written content
    @Test
    public void sequenceOfCustomClass() {
        final List<Foo> list = Arrays.asList(new Foo(0), new Foo(1), new Foo(2));
        wire.getValueOut().sequence(list);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Custom class definition with a string value for testing
    final static class Bar {

        final String value;

        public Bar(String value) {
            this.value = value;
        }

    }

    // Test to write a custom class Bar instance to the wire and verify the written content
    @Test
    public void customClass() {
        wire.getValueOut().object(new Bar("Bazz"));
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Test to write a Duration instance to the wire and verify the written content
    @Test
    public void duration() {
        final Duration duration = Duration.ofSeconds(63);
        wire.getValueOut().object(duration);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

    // Custom class implementing Serializable interface for testing
    static final class Ser implements Serializable {
        private static final long serialVersionUID = 0L;
        int foo;
    }

    // Test to write a serializable class instance to the wire and verify the written content
    @Test
    public void serializable() {
        final Ser s = new Ser();
        wire.getValueOut().object(s);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
    }

}
