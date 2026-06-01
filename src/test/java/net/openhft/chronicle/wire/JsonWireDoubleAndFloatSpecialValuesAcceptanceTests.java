/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.DoublePredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Acceptance tests for JSON Wire Double and Float value handling. Non-finite values (NaN,
 * Infinity) are quoted as JSON string literals; finite values of every magnitude are written
 * as unquoted JSON numbers whose token denotes the exact value (CORE-64), verified across both
 * sides of every formatter threshold.
 * <p>
 * For implementation details please see:
 * <ul>
 *     <li>{@link YamlWireOut.YamlValueOut#float64(double)}</li>
 *     <li>{@link YamlWireOut.YamlValueOut#float32(float)}</li>
 *     <li>{@link YamlWire.YamlValueOut#writeSpecialDoubleValueToBytes(Bytes, double)} </li>
 *     <li>{@link YamlWire.YamlValueOut#writeSpecialFloatValueToBytes(Bytes, float)} </li>
 *     <li>{@link JSONWire.JSONValueOut#writeSpecialDoubleValueToBytes(Bytes, double)} </li>
 *     <li>{@link JSONWire.JSONValueOut#writeSpecialFloatValueToBytes(Bytes, float)} </li>
 * </ul>
 */
class JsonWireDoubleAndFloatSpecialValuesAcceptanceTests {

    @ParameterizedTest
    @MethodSource("doubleTestInputs")
    void serialiseDoubleSpecialValues(DoubleTestInput doubleTestInput) {
        assertEquals(
                String.format("\"%s\"", doubleTestInput.expectedStringRepresentation),
                toJson(doubleTestInput.inputValue),
                "Expected correct representation for special value and for it to be quoted as a string literal"
        );
    }

    @ParameterizedTest
    @MethodSource("floatTestInputs")
    void serialiseFloatSpecialValues(FloatTestInput floatTestInput) {
        assertEquals(
                String.format("\"%s\"", floatTestInput.expectedStringRepresentation),
                toJson(floatTestInput.inputValue),
                "Expected correct representation for special value and for it to be quoted as a string literal"
        );
    }

    @ParameterizedTest
    @MethodSource("doubleTestInputs")
    void doubleRoundTrip(DoubleTestInput doubleTestInput) {
        // Serialise an object to JSON and ensure its represented correctly
        JSONWire inputWire = new JSONWire();
        inputWire.getValueOut().object(new DoubleDto(doubleTestInput.inputValue));
        String text = JSONWire.asText(inputWire);
        assertEquals(
                String.format("{\"value\":\"%s\"}", doubleTestInput.expectedStringRepresentation),
                text,
                "Expected JSON representation where special values are quoted string literals"
        );

        // Deserialize back to an object, ensure that the special value is retained
        JSONWire outputWire = JSONWire.from(text);
        DoubleDto object = outputWire.getValueIn().object(DoubleDto.class);
        Assertions.assertNotNull(object);
        Assertions.assertTrue(doubleTestInput.expectOutputDoubleToMatchThisPredicate.test(object.value));
    }

    @ParameterizedTest
    @MethodSource("floatTestInputs")
    void floatRoundTrip(FloatTestInput floatTestInput) {
        // Serialise an object to JSON and ensure its represented correctly
        JSONWire inputWire = new JSONWire();
        inputWire.getValueOut().object(new FloatDto(floatTestInput.inputValue));
        String text = JSONWire.asText(inputWire);
        assertEquals(
                String.format("{\"value\":\"%s\"}", floatTestInput.expectedStringRepresentation),
                text,
                "Expected JSON representation where special values are quoted string literals"
        );

        // Deserialize back to an object, ensure that the special value is retained
        JSONWire outputWire = JSONWire.from(text);
        FloatDto object = outputWire.getValueIn().object(FloatDto.class);
        Assertions.assertNotNull(object);
        Assertions.assertTrue(floatTestInput.expectOutputFloatToMatchThisPredicate.test(object.value));
    }

    // ------------------------------------------------------------------------------------------
    // Finite values must serialise as unquoted JSON numbers (CORE-64). The faithfulness contract
    // is verified against the reference JDK parser, independent of Chronicle's own reader: the
    // emitted token, parsed by Double/Float.parseDouble, must denote the exact input. This holds
    // for every finite magnitude and for both sides of every formatter threshold (stepped by one
    // ulp). End-to-end round-trip through the wire's own reader is asserted separately, on the
    // values the current reader handles exactly.
    // ------------------------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("finiteFloatInputs")
    void finiteFloatSerialisesAsFaithfulJsonNumber(float value) {
        assertFloatIsFaithfulJsonNumber(value);
    }

    @ParameterizedTest
    @MethodSource("finiteDoubleInputs")
    void finiteDoubleSerialisesAsFaithfulJsonNumber(double value) {
        assertDoubleIsFaithfulJsonNumber(value);
    }

    @ParameterizedTest
    @MethodSource("finiteFloatInputs")
    void finiteFloatRoundTripsThroughWire(float value) {
        // float carries ~7 significant digits, well within the parser's precision, so every
        // finite float round-trips exactly through the wire's own reader.
        FloatDto object = JSONWire.from(toFieldJson(new FloatDto(value))).getValueIn().object(FloatDto.class);
        Assertions.assertNotNull(object);
        assertEquals(value, object.value, 0.0f, "Finite float " + value + " must round-trip through the wire");
    }

    @ParameterizedTest
    @MethodSource("wireRoundTripDoubleInputs")
    void finiteDoubleRoundTripsThroughWire(double value) {
        // Scoped to values the current reader restores exactly. Full-precision (~16-17 digit)
        // doubles can lose the last ulp in Bytes.parseDouble (chronicle-bytes) — the writer emits
        // a faithful token (asserted above), but the wire reader rounds it imperfectly. Widening
        // this set is deferred to the Bytes.parseDouble triage/fix.
        DoubleDto object = JSONWire.from(toFieldJson(new DoubleDto(value))).getValueIn().object(DoubleDto.class);
        Assertions.assertNotNull(object);
        assertEquals(value, object.value, 0.0, "Finite double " + value + " must round-trip through the wire");
    }

    /**
     * Finite floats spanning the {@link YamlWireOut.YamlValueOut#float32(float)} formatter: both
     * sides of each {@code [1e-3, 1e6)} fast-path threshold (stepped by one ulp), large and small
     * magnitudes, the extreme finite values, and negatives.
     */
    private static Stream<Float> finiteFloatInputs() {
        return Stream.of(
                // upper threshold 1e6, one ulp either side
                1e6f - Math.ulp(1e6f),      // fast path (bytes.append)
                1e6f,                        // special path (1e6 < 1e6 is false)
                1e6f + Math.ulp(1e6f),      // special path
                // lower threshold 1e-3, one ulp either side
                1e-3f - Math.ulp(1e-3f),    // special path
                1e-3f,                       // fast path (1e-3 >= 1e-3 is true)
                1e-3f + Math.ulp(1e-3f),    // fast path
                // large and small magnitudes
                5_000_000.0f, 1e7f, 1.23456789e10f, Float.MAX_VALUE,
                1e-4f, 1e-7f, Float.MIN_VALUE,
                // negatives
                -1e6f, -5_000_000.0f, -Float.MAX_VALUE, -1e-4f,
                // round values
                2_000_000.0f, 3_000.0f
        );
    }

    /**
     * Finite doubles spanning the {@link YamlWireOut.YamlValueOut#float64(double)} formatter: both
     * sides of each {@code [1e-3, 1e15)} fast-path threshold (stepped by one ulp), the round-number
     * {@code E3}/{@code E6} branches, full-precision mid-range values, the extreme finite values,
     * the small magnitudes that route to the faithful Double.toString path (CORE-64), and negatives.
     */
    private static Stream<Double> finiteDoubleInputs() {
        return Stream.of(
                // upper threshold 1e15, one ulp either side
                1e15 - Math.ulp(1e15),      // fast path (bytes.append)
                1e15,                        // special path (1e15 < 1e15 is false)
                1e15 + Math.ulp(1e15),      // special path
                // lower threshold 1e-3, one ulp either side
                1e-3 - Math.ulp(1e-3),      // special path (faithful Double.toString)
                1e-3,                        // fast path (1e-3 >= 1e-3 is true)
                1e-3 + Math.ulp(1e-3),      // fast path
                // round-number E3 / E6 fast-path branches
                3_000.0, 2_000_000.0,
                // full-precision values inside the append branches — writer must stay faithful
                123.45678901234567, 9.999999999999998e14,
                // large and small magnitudes
                1e16, 1.23456789e20, 5e15, Double.MAX_VALUE,
                1e-4, 1e-7, 1e-8, 1e-300, Double.MIN_VALUE,
                // negatives
                -1e15, -5e15, -Double.MAX_VALUE, -1e-4
        );
    }

    /**
     * Doubles whose faithful token the current wire reader also restores exactly (few significant
     * digits, or clean powers of ten). See {@link #finiteDoubleRoundTripsThroughWire(double)} for
     * why full-precision doubles are excluded pending the Bytes.parseDouble fix.
     */
    private static Stream<Double> wireRoundTripDoubleInputs() {
        return Stream.of(
                1e15, 1e16, 5e15, -1e15, 1.23456789e20, Double.MAX_VALUE,
                1e-4, 1e-7, 1e-8, 1e-300, Double.MIN_VALUE, -1e-4,
                3_000.0, 2_000_000.0, 0.5, 1.5, 100.25, -42.0
        );
    }

    /** A finite float must serialise as an unquoted JSON number whose token denotes the exact value. */
    private void assertFloatIsFaithfulJsonNumber(float value) {
        String scalar = toJson(value);
        Assertions.assertFalse(scalar.startsWith("\""),
                "Finite float " + value + " must serialise as an unquoted JSON number, got " + scalar);
        assertEquals(value, Float.parseFloat(scalar), 0.0f,
                "Scalar JSON token must denote the exact float, got " + scalar);
        String field = toFieldJson(new FloatDto(value));
        Assertions.assertFalse(field.contains("\"value\":\""),
                "Finite float " + value + " must be written as a JSON number field, got " + field);
        assertEquals(value, Float.parseFloat(fieldNumber(field)), 0.0f,
                "Field JSON token must denote the exact float, got " + field);
    }

    /** A finite double must serialise as an unquoted JSON number whose token denotes the exact value. */
    private void assertDoubleIsFaithfulJsonNumber(double value) {
        String scalar = toJson(value);
        Assertions.assertFalse(scalar.startsWith("\""),
                "Finite double " + value + " must serialise as an unquoted JSON number, got " + scalar);
        assertEquals(value, Double.parseDouble(scalar), 0.0,
                "Scalar JSON token must denote the exact double, got " + scalar);
        String field = toFieldJson(new DoubleDto(value));
        Assertions.assertFalse(field.contains("\"value\":\""),
                "Finite double " + value + " must be written as a JSON number field, got " + field);
        assertEquals(value, Double.parseDouble(fieldNumber(field)), 0.0,
                "Field JSON token must denote the exact double, got " + field);
    }

    /** Serialise a DTO to its JSON field representation, e.g. {@code {"value":1.0E16}}. */
    private static String toFieldJson(Marshallable dto) {
        JSONWire wire = new JSONWire();
        wire.getValueOut().object(dto);
        return JSONWire.asText(wire);
    }

    /** Extract the numeric token from a single-field document, e.g. {@code {"value":1.0E16}} -> {@code 1.0E16}. */
    private static String fieldNumber(String fieldJson) {
        return fieldJson.substring(fieldJson.indexOf(':') + 1, fieldJson.lastIndexOf('}'));
    }

    private static Stream<DoubleTestInput> doubleTestInputs() {
        return Stream.of(
                new DoubleTestInput(Double.NaN, "NaN", Double::isNaN),
                new DoubleTestInput(Double.NEGATIVE_INFINITY, "-Infinity", Double::isInfinite),
                new DoubleTestInput(Double.POSITIVE_INFINITY, "Infinity", Double::isInfinite)
        );
    }

    private static class DoubleTestInput {
        private final double inputValue;
        private final String expectedStringRepresentation;

        private final DoublePredicate expectOutputDoubleToMatchThisPredicate;

        private DoubleTestInput(double inputValue,
                                String expectedStringRepresentation,
                                DoublePredicate expectOutputDoubleToMatchThisPredicate) {
            this.inputValue = inputValue;
            this.expectedStringRepresentation = expectedStringRepresentation;
            this.expectOutputDoubleToMatchThisPredicate = expectOutputDoubleToMatchThisPredicate;
        }

        @Override
        public String toString() {
            return "DoubleTestInput{" +
                    "value=" + inputValue +
                    ", expectedRepresentation='" + expectedStringRepresentation + '\'' +
                    '}';
        }

    }

    private static Stream<FloatTestInput> floatTestInputs() {
        return Stream.of(
                new FloatTestInput(Float.NaN, "NaN", Double::isNaN),
                new FloatTestInput(Float.NEGATIVE_INFINITY, "-Infinity", Double::isInfinite),
                new FloatTestInput(Float.POSITIVE_INFINITY, "Infinity", Double::isInfinite)
        );
    }

    private static class FloatTestInput {

        private final float inputValue;
        private final String expectedStringRepresentation;
        private final Predicate<Float> expectOutputFloatToMatchThisPredicate; // No dedicated FloatPredicate in JDK

        private FloatTestInput(float inputValue,
                               String expectedStringRepresentation,
                               Predicate<Float> expectOutputFloatToMatchThisPredicate) {
            this.inputValue = inputValue;
            this.expectedStringRepresentation = expectedStringRepresentation;
            this.expectOutputFloatToMatchThisPredicate = expectOutputFloatToMatchThisPredicate;
        }

        @Override
        public String toString() {
            return "FloatTestInput{" +
                    "value=" + inputValue +
                    ", expectedRepresentation='" + expectedStringRepresentation + '\'' +
                    '}';
        }

    }

    /**
     * Convert the double value to its JSON string representation.
     */
    private String toJson(double value) {
        JSONWire jsonWire = new JSONWire();
        jsonWire.getValueOut().object(value);
        return JSONWire.asText(jsonWire);
    }

    /**
     * Convert the float value to its JSON string representation.
     */
    private String toJson(float value) {
        JSONWire jsonWire = new JSONWire();
        jsonWire.getValueOut().object(value);
        return JSONWire.asText(jsonWire);
    }

    /**
     * Simple DTO for testing double serialise/deserialize to/from JSON.
     */
    private static class DoubleDto extends SelfDescribingMarshallable {

        private final double value;

        private DoubleDto(double value) {
            this.value = value;
        }
    }

    /**
     * Simple DTO for testing float serialise/deserialize to/from JSON.
     */
    private static class FloatDto extends SelfDescribingMarshallable {

        private final float value;

        private FloatDto(float value) {
            this.value = value;
        }
    }

}
