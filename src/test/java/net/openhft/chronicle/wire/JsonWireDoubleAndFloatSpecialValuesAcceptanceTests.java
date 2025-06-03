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
 * Acceptance tests for JSON Wire Double and Float special value handling, e.g. NaN, Infinity.
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
public class JsonWireDoubleAndFloatSpecialValuesAcceptanceTests {

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
    public String toJson(double value) {
        JSONWire jsonWire = new JSONWire();
        jsonWire.getValueOut().object(value);
        return JSONWire.asText(jsonWire);
    }

    /**
     * Convert the float value to its JSON string representation.
     */
    public String toJson(float value) {
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
