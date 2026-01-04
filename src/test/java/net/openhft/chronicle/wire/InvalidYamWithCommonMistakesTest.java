/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression coverage for common YAML formatting mistakes that should still parse.
 * Tests that minor syntax issues remain tolerated where practical.
 */
public class InvalidYamWithCommonMistakesTest extends WireTestCommon {

    // Test to verify the parsing of a DTO from a string representation
    @Test
    @DisplayName("Parses dto with missing space after colon")
    public void testDtp() {

        // Expected DTO object with value "hello8"
        DtoB expected = new DtoB("hello8");

        // Parsing the DTO object from a string representation
        Marshallable actual = Marshallable.fromString("!net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$DtoB " +
                "{\n" +
                "  y:hello8\n" +
                "}\n");
        // Assert that the parsed DTO matches the expected DTO
        assertEquals(expected, actual,
                "dto should parse despite missing space after colon");
    }

    // Test to assume the type of DTO and parse it
    @Test
    @DisplayName("Parses dto with assumed type and missing space")
    public void testAssumeTheType() {

        // Expected DTO object with value "hello8"
        DtoB expected = new DtoB("hello8");

        // Parsing the DTO object from a string representation while assuming its type
        Marshallable actual = Marshallable.fromString(DtoB.class, "!InvalidYamWithCommonMistakesTest$DtoB " +
                "{\n" +
                "  y:hello8\n" +
                "}\n");
        // Assert that the parsed DTO matches the expected DTO
        assertEquals(expected, actual,
                "dto should parse with assumed type and missing space");
    }

    // Test to assume the type of DTO without mentioning the full class path and parse it
    @Test
    @DisplayName("Parses dto with short type name")
    public void testAssumeTheType2() {

        // Expected DTO object with value "hello8"
        DtoB expected = new DtoB("hello8");

        // Parsing the DTO object from a string representation while assuming its short type name
        Marshallable actual = Marshallable.fromString(DtoB.class, "!DtoB " +
                "{\n" +
                "  y:hello8\n" +
                "}\n");

        // Assert that the parsed DTO matches the expected DTO
        assertEquals(expected, actual,
                "dto should parse with short type name");
    }

    @Test
    @DisplayName("Rejects missing type when tuple generation disabled")
    public void testAssumeTheTypeMissingTypeThrows() {
        assertThrows(ClassNotFoundRuntimeException.class, () -> {
            Wires.setGenerateTuples(false);

            final String cs = "!Xyz " +
                    "{\n" +
                    "  y: hello8\n" +
                    "}\n";
            String s = Marshallable.fromString(Dto.class, cs).toString();
            assertEquals("!net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$Dto {\n" +
                    "  y: hello8,\n" +
                    "  x: !!null \"\"\n" +
                    "}\n", s,
                    "dto should parse with default null for missing nested type");
        }, "missing type should throw ClassNotFoundRuntimeException");
    }

    // Test to parse a DTO with nested types
    @Test
    @DisplayName("Parses dto with nested mapping without type prefix")
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
        assertEquals(expected, actual,
                "dto should parse nested mapping without type prefix");
    }

    // Test to parse a DTO with incorrect nested type definition
    @Test
    @DisplayName("Parses dto with explicit nested type")
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
        assertEquals(expected, actual,
                "dto should parse with explicit nested type");

    }

    // Test to assume the type based on the type details provided within the YAML string
    @Test
    @DisplayName("Parses dto from yaml with explicit type name")
    public void testAssumeTypeBasedOnWhatIsIntheYaml() {

        // Expected DtoB object with value "hello8"
        DtoB expected = new DtoB("hello8");

        // Parse DtoB object from a string representation containing explicit class path details
        DtoB actual = Marshallable.fromString("!net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$DtoB " +
                "{\n" +
                "  y:hello8\n" +
                "}\n");

        // Assert that the parsed object matches the expected object
        assertEquals(expected, actual,
                "dto should parse from explicit type name in yaml");
    }

    // Test to assume the type based on the class provided and the YAML string
    @Test
    @DisplayName("Parses dto when type is supplied separately")
    public void testAssumeTypeBasedOnWhatIsIntheYaml3() {

        // Expected DtoB object with value "hello8"
        DtoB expected = new DtoB("hello8");

        // Parse DtoB object from a string representation, type assumed from the given class
        DtoB actual = Marshallable.fromString(DtoB.class, "{\n" +
                "  y:hello8\n" +
                "}\n");

        // Assert that the parsed object matches the expected object
        assertEquals(expected, actual,
                "dto should parse when class type is provided");
    }

    // Test to assume the type based on the type details within the YAML string containing a space
    @Test
    @DisplayName("Parses dto with leading space before type tag")
    public void testAssumeTypeBasedOnWhatIsIntheYamlWithSpace() {

        // Expected DtoB object with value "hello8"
        DtoB expected = new DtoB("hello8");

        // Parse object from a string representation containing space before explicit class path details
        Object actual = Marshallable.<DtoB>fromString(" !net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$DtoB " +
                "{\n" +
                "  y:hello8\n" +
                "}\n");

        // Assert that the parsed object matches the expected object
        assertEquals(expected, actual,
                "dto should parse with leading space before type tag");
    }

    // Test to assume the type based on the type details within the YAML string containing a space at a different position
    @Test
    @DisplayName("Parses dto with space before opening brace")
    public void testAssumeTypeBasedOnWhatIsIntheYamlWithSpace2() {

        // Expected DtoB object with value "hello8"
        DtoB expected = new DtoB("hello8");

        // Parse object from a string representation containing space just before the curly brace
        Object actual = Marshallable.<DtoB>fromString(" !net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$DtoB {\n" +
                "  y:hello8\n" +
                "}\n");

        // Assert that the parsed object matches the expected object
        assertEquals(expected, actual,
                "dto should parse with space before opening brace");
    }

    // Test to assume the type based on an alias instead of the full type name
    @Test
    @DisplayName("DTO should parse using registered alias")
    public void testAssumeTypeBasedOnWhatButUseAlias() {

        // Add alias for DtoB class
        ClassAliasPool.CLASS_ALIASES.addAlias(DtoB.class);

        // Expected DtoB object with value "hello8"
        DtoB expected = new DtoB("hello8");

        // Parse DtoB object from a string representation using the alias
        DtoB actual = Marshallable.fromString("!DtoB{\n" +
                "  y:hello8\n" +
                "}\n");

        // Assert that the parsed object matches the expected object
        assertEquals(expected, actual,
                "dto should parse using alias");
    }

    // DTO class containing a string and another DTO
    static class Dto extends SelfDescribingMarshallable {
        final String y;
        final DtoB x;

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
        DtoB(final String y) {
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
