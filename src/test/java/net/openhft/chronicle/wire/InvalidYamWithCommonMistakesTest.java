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

import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by Rob Austin
 * <p>
 * Tests that common mistakes are still parsed where we can
 */
public class InvalidYamWithCommonMistakesTest extends WireTestCommon {

    // Test to verify the parsing of a DTO from a string representation
    @Test
    public void testDtp() {

        // Expected DTO object with value "hello8"
        DtoB expected = new DtoB("hello8");

        // Parsing the DTO object from a string representation
        Marshallable actual = Marshallable.fromString("!net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$DtoB " +
                "{\n" +
                "  y:hello8\n" +
                "}\n");
        // Assert that the parsed DTO matches the expected DTO
        assertEquals(expected, actual);
    }

    // Test to assume the type of DTO and parse it
    @Test
    public void testAssumeTheType() {

        // Expected DTO object with value "hello8"
        DtoB expected = new DtoB("hello8");

        // Parsing the DTO object from a string representation while assuming its type
        Marshallable actual = Marshallable.fromString(DtoB.class, "!InvalidYamWithCommonMistakesTest$DtoB " +
                "{\n" +
                "  y:hello8\n" +
                "}\n");
        // Assert that the parsed DTO matches the expected DTO
        assertEquals(expected, actual);
    }

    // Test to assume the type of DTO without mentioning the full class path and parse it
    @Test
    public void testAssumeTheType2() {

        // Expected DTO object with value "hello8"
        DtoB expected = new DtoB("hello8");

        // Parsing the DTO object from a string representation while assuming its short type name
        Marshallable actual = Marshallable.fromString(DtoB.class, "!DtoB " +
                "{\n" +
                "  y:hello8\n" +
                "}\n");

        // Assert that the parsed DTO matches the expected DTO
        assertEquals(expected, actual);
    }

    @Test(expected = ClassNotFoundRuntimeException.class)
    public void testAssumeTheTypeMissingTypeThrows() {
        Wires.GENERATE_TUPLES = false;

        final String cs = "!Xyz " +
                "{\n" +
                "  y: hello8\n" +
                "}\n";
        String s = Marshallable.fromString(Dto.class, cs).toString();
        assertEquals("" +
                "!net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$Dto {\n" +
                "  y: hello8,\n" +
                "  x: !!null \"\"\n" +
                "}\n", s);
    }

    // Test to parse a DTO with nested types
    @Test
    public void testBadTypeDtp0() {

        // Expected DTO object with values "hello" and "c"
        Dto expected = new Dto("hello", new DtoB("c"));

        // Parsing the nested DTO object from a string representation
        Dto actual = Marshallable.fromString("!net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$Dto {\n" +
                "  x:{\n" + // strickly speaking this
                "    y: c\n" +
                "  }\n" +
                "  y: hello,\n" +
                "}");

        // Assert that the parsed DTO matches the expected DTO
        assertEquals(expected, actual);
    }

    // Test to parse a DTO with incorrect nested type definition
    @Test
    public void testBadTypeDtpBadType() {

        // Expected DTO object with values "hello" and "c"
        Dto expected = new Dto("hello", new DtoB("c"));

        // Parsing the nested DTO object from a string representation with incorrect type definition
        Dto actual = Marshallable.fromString(Dto.class, " {\n" +
                "  x: !net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$DtoB {\n" + //
                // strickly speaking this
                "    y: c\n" +
                "  }\n" +
                "  y: hello,\n" +
                "}");

        // Assert that the parsed DTO matches the expected DTO
        assertEquals(expected, actual);

    }

    // Test to assume the type based on the type details provided within the YAML string
    @Test
    public void testAssumeTypeBasedOnWhatIsIntheYaml() {

        // Expected DtoB object with value "hello8"
        DtoB expected = new DtoB("hello8");

        // Parse DtoB object from a string representation containing explicit class path details
        DtoB actual = Marshallable.<DtoB>fromString("!net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$DtoB " +
                "{\n" +
                "  y:hello8\n" +
                "}\n");

        // Assert that the parsed object matches the expected object
        assertEquals(expected, actual);
    }

    // Test to assume the type based on the class provided and the YAML string
    @Test
    public void testAssumeTypeBasedOnWhatIsIntheYaml3() {

        // Expected DtoB object with value "hello8"
        DtoB expected = new DtoB("hello8");

        // Parse DtoB object from a string representation, type assumed from the given class
        DtoB actual = Marshallable.<DtoB>fromString(DtoB.class, "{\n" +
                "  y:hello8\n" +
                "}\n");

        // Assert that the parsed object matches the expected object
        assertEquals(expected, actual);
    }

    // Test to assume the type based on the type details within the YAML string containing a space
    @Test
    public void testAssumeTypeBasedOnWhatIsIntheYamlWithSpace() {

        // Expected DtoB object with value "hello8"
        DtoB expected = new DtoB("hello8");

        // Parse object from a string representation containing space before explicit class path details
        Object actual = Marshallable.<DtoB>fromString(" !net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$DtoB " +
                "{\n" +
                "  y:hello8\n" +
                "}\n");

        // Assert that the parsed object matches the expected object
        assertEquals(expected, actual);
    }

    // Test to assume the type based on the type details within the YAML string containing a space at a different position
    @Test
    public void testAssumeTypeBasedOnWhatIsIntheYamlWithSpace2() {

        // Expected DtoB object with value "hello8"
        DtoB expected = new DtoB("hello8");

        // Parse object from a string representation containing space just before the curly brace
        Object actual = Marshallable.<DtoB>fromString(" !net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$DtoB {\n" +
                "  y:hello8\n" +
                "}\n");

        // Assert that the parsed object matches the expected object
        assertEquals(expected, actual);
    }

    // Test to assume the type based on an alias instead of the full type name
    @Test
    public void testAssumeTypeBasedOnWhatButUseAlias() {

        // Add alias for DtoB class
        ClassAliasPool.CLASS_ALIASES.addAlias(DtoB.class);

        // Expected DtoB object with value "hello8"
        DtoB expected = new DtoB("hello8");

        // Parse DtoB object from a string representation using the alias
        DtoB actual = Marshallable.<DtoB>fromString("!DtoB{\n" +
                "  y:hello8\n" +
                "}\n");

        // Assert that the parsed object matches the expected object
        assertEquals(expected, actual);
    }

    // DTO class containing a string and another DTO
    public static class Dto extends SelfDescribingMarshallable {
        String y;
        DtoB x;

        // Constructor to initialize DTO with given values
        Dto(final String y, final DtoB x) {
            this.y = y;
            this.x = x;
        }

        // Getter method for the 'y' property
        String y() {
            return y;
        }

        // Getter method for the 'x' property
        DtoB x() {
            return x;
        }
    }

    // DTO class containing a string property
    public static class DtoB extends SelfDescribingMarshallable {
        String y;

        // Constructor to initialize DtoB with given value
        public DtoB(final String y) {
            this.y = y;
        }

        // Getter method for the 'y' property
        String y() {
            return y;
        }

        // Setter method for the 'y' property
        public DtoB y(final String y) {
            this.y = y;
            return this;
        }
    }
}
