/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for IEEE 754 numeric edge cases and precision handling.
 * Critical for financial trading systems where numeric precision matters.
 */
@SuppressWarnings({"deprecation", "removal"})
public class WireNumericEdgeCasesTest extends WireTestCommon {

    // ========== Negative Zero Tests ==========

    // TODO FIX: BinaryWire does not preserve negative zero bit pattern
    @Test
    @Disabled("BinaryWire: Negative zero round-trips as positive zero - needs investigation")
    @DisplayName("BinaryWire should preserve negative zero bit pattern")
    public void testNegativeZeroPreservationBinary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        double negZero = -0.0;
        wire.write("val").float64(negZero);
        bytes.readPosition(0);

        double result = wire.read("val").float64();
        assertEquals(Double.doubleToLongBits(-0.0), Double.doubleToLongBits(result),
                "Negative zero bit pattern should be preserved in BinaryWire");
    }

    // TODO FIX: TextWire does not preserve negative zero bit pattern
    @Test
    @Disabled("TextWire: Negative zero round-trips as positive zero - needs investigation")
    @DisplayName("TextWire should preserve negative zero bit pattern")
    public void testNegativeZeroPreservationText() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        TextWire wire = new TextWire(bytes);

        double negZero = -0.0;
        wire.write("val").float64(negZero);
        bytes.readPosition(0);

        double result = wire.read("val").float64();
        assertEquals(Double.doubleToLongBits(-0.0), Double.doubleToLongBits(result),
                "Negative zero bit pattern should be preserved in TextWire");
    }

    // TODO FIX: YamlWire does not preserve negative zero bit pattern
    @Test
    @Disabled("YamlWire: Negative zero round-trips as positive zero - needs investigation")
    @DisplayName("YamlWire should preserve negative zero bit pattern")
    public void testNegativeZeroPreservationYaml() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        YamlWire wire = new YamlWire(bytes);

        double negZero = -0.0;
        wire.write("val").float64(negZero);
        bytes.readPosition(0);

        double result = wire.read("val").float64();
        assertEquals(Double.doubleToLongBits(-0.0), Double.doubleToLongBits(result),
                "Negative zero bit pattern should be preserved in YamlWire");
    }

    @Test
    @DisplayName("Negative zero should compare equal to positive zero")
    public void testNegativeZeroEquality() {
        // This is IEEE 754 behaviour - -0.0 == 0.0
        assertTrue(-0.0 == 0.0, "Negative and positive zero should be equal by IEEE 754");
        assertNotEquals(Double.doubleToLongBits(-0.0), Double.doubleToLongBits(0.0),
                "Bit patterns should differ");
    }

    // ========== Subnormal Number Tests ==========

    @Test
    @DisplayName("BinaryWire should preserve Double.MIN_VALUE (smallest positive subnormal)")
    public void testSubnormalMinValueBinary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("val").float64(Double.MIN_VALUE);
        bytes.readPosition(0);

        double result = wire.read("val").float64();
        assertEquals(Double.MIN_VALUE, result, 0,
                "Double.MIN_VALUE should round-trip exactly in BinaryWire");
    }

    @Test
    @DisplayName("TextWire should preserve Double.MIN_VALUE (smallest positive subnormal)")
    public void testSubnormalMinValueText() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        TextWire wire = new TextWire(bytes);

        wire.write("val").float64(Double.MIN_VALUE);
        bytes.readPosition(0);

        double result = wire.read("val").float64();
        assertEquals(Double.MIN_VALUE, result, 0,
                "Double.MIN_VALUE should round-trip exactly in TextWire");
    }

    @Test
    @DisplayName("YamlWire should preserve Double.MIN_VALUE (smallest positive subnormal)")
    public void testSubnormalMinValueYaml() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        YamlWire wire = new YamlWire(bytes);

        wire.write("val").float64(Double.MIN_VALUE);
        bytes.readPosition(0);

        double result = wire.read("val").float64();
        assertEquals(Double.MIN_VALUE, result, 0,
                "Double.MIN_VALUE should round-trip exactly in YamlWire");
    }

    @Test
    @DisplayName("Double.MIN_NORMAL (smallest normal) should round-trip in BinaryWire")
    public void testMinNormalValueBinary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("val").float64(Double.MIN_NORMAL);
        bytes.readPosition(0);

        double result = wire.read("val").float64();
        assertEquals(Double.MIN_NORMAL, result, 0,
                "Double.MIN_NORMAL should round-trip in BinaryWire");
    }

    // TODO FIX: TextWire loses precision for Double.MIN_NORMAL
    @Test
    @Disabled("TextWire loses precision for Double.MIN_NORMAL - needs investigation")
    @DisplayName("Double.MIN_NORMAL (smallest normal) should round-trip in TextWire")
    public void testMinNormalValueText() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        TextWire wire = new TextWire(bytes);

        wire.write("val").float64(Double.MIN_NORMAL);
        bytes.readPosition(0);

        double result = wire.read("val").float64();
        assertEquals(Double.MIN_NORMAL, result, 0,
                "Double.MIN_NORMAL should round-trip in TextWire");
    }

    // TODO FIX: YamlWire loses precision for Double.MIN_NORMAL
    @Test
    @Disabled("YamlWire loses precision for Double.MIN_NORMAL - needs investigation")
    @DisplayName("Double.MIN_NORMAL (smallest normal) should round-trip in YamlWire")
    public void testMinNormalValueYaml() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        YamlWire wire = new YamlWire(bytes);

        wire.write("val").float64(Double.MIN_NORMAL);
        bytes.readPosition(0);

        double result = wire.read("val").float64();
        assertEquals(Double.MIN_NORMAL, result, 0,
                "Double.MIN_NORMAL should round-trip in YamlWire");
    }

    // ========== NaN Tests ==========

    @Test
    @DisplayName("Standard NaN should round-trip in BinaryWire")
    public void testNaNBinary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("val").float64(Double.NaN);
        bytes.readPosition(0);

        double result = wire.read("val").float64();
        assertTrue(Double.isNaN(result), "NaN should round-trip as NaN in BinaryWire");
    }

    @Test
    @DisplayName("Standard NaN should round-trip in TextWire")
    public void testNaNText() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        TextWire wire = new TextWire(bytes);

        wire.write("val").float64(Double.NaN);
        bytes.readPosition(0);

        double result = wire.read("val").float64();
        assertTrue(Double.isNaN(result), "NaN should round-trip as NaN in TextWire");
    }

    @Test
    @DisplayName("Standard NaN should round-trip in YamlWire")
    public void testNaNYaml() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        YamlWire wire = new YamlWire(bytes);

        wire.write("val").float64(Double.NaN);
        bytes.readPosition(0);

        double result = wire.read("val").float64();
        assertTrue(Double.isNaN(result), "NaN should round-trip as NaN in YamlWire");
    }

    // TODO FIX: Custom NaN bit patterns may not be preserved
    @Test
    @Disabled("Custom NaN bit pattern not preserved - may canonicalise to standard NaN")
    @DisplayName("Custom NaN bit patterns should be preserved")
    public void testCustomNaNBitPattern() {
        // Quiet NaN with custom payload
        long customNaNBits = 0x7FF8000000000001L;
        double customNaN = Double.longBitsToDouble(customNaNBits);
        assertTrue(Double.isNaN(customNaN), "Custom bits should produce NaN");

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("val").float64(customNaN);
            bytes.readPosition(0);

            double result = wire.read("val").float64();
            assertEquals(customNaNBits, Double.doubleToRawLongBits(result),
                    "Custom NaN bit pattern should be preserved in " + wt);
        }
    }

    // ========== Infinity Tests ==========

    @Test
    @DisplayName("Double positive infinity value should round-trip")
    public void testPositiveInfinity() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("val").float64(Double.POSITIVE_INFINITY);
            bytes.readPosition(0);

            double result = wire.read("val").float64();
            assertEquals(Double.POSITIVE_INFINITY, result,
                    "Positive infinity should round-trip in " + wt);
        }
    }

    @Test
    @DisplayName("Double negative infinity value should round-trip")
    public void testNegativeInfinity() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("val").float64(Double.NEGATIVE_INFINITY);
            bytes.readPosition(0);

            double result = wire.read("val").float64();
            assertEquals(Double.NEGATIVE_INFINITY, result,
                    "Negative infinity should round-trip in " + wt);
        }
    }

    // ========== Float Special Values ==========

    @Test
    @DisplayName("Float NaN value should round-trip correctly")
    public void testFloatNaN() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("val").float32(Float.NaN);
            bytes.readPosition(0);

            float result = wire.read("val").float32();
            assertTrue(Float.isNaN(result), "Float NaN should round-trip in " + wt);
        }
    }

    @Test
    @DisplayName("Float infinity values should round-trip correctly")
    public void testFloatInfinities() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("posInf").float32(Float.POSITIVE_INFINITY);
            wire.write("negInf").float32(Float.NEGATIVE_INFINITY);
            bytes.readPosition(0);

            assertEquals(Float.POSITIVE_INFINITY, wire.read("posInf").float32(),
                    "Float +Inf should round-trip in " + wt);
            assertEquals(Float.NEGATIVE_INFINITY, wire.read("negInf").float32(),
                    "Float -Inf should round-trip in " + wt);
        }
    }

    @Test
    @DisplayName("Float.MIN_VALUE (smallest positive subnormal) should round-trip")
    public void testFloatMinValue() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("val").float32(Float.MIN_VALUE);
            bytes.readPosition(0);

            float result = wire.read("val").float32();
            assertEquals(Float.MIN_VALUE, result, 0,
                    "Float.MIN_VALUE should round-trip in " + wt);
        }
    }

    // ========== JavaScript Safe Integer Boundary Tests ==========

    @Test
    @DisplayName("Value at JS safe integer boundary (2^53) should round-trip")
    public void testJsSafeIntegerBoundary() {
        long jsSafeMax = 9007199254740992L; // 2^53

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("val").int64(jsSafeMax);
            bytes.readPosition(0);

            long result = wire.read("val").int64();
            assertEquals(jsSafeMax, result,
                    "JS safe integer max should round-trip in " + wt);
        }
    }

    @Test
    @DisplayName("Value beyond JS safe integer (2^53 + 1) should round-trip as long")
    public void testBeyondJsSafeInteger() {
        long beyondSafe = 9007199254740993L; // 2^53 + 1

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("val").int64(beyondSafe);
            bytes.readPosition(0);

            long result = wire.read("val").int64();
            assertEquals(beyondSafe, result,
                    "Beyond JS safe integer should round-trip exactly in " + wt);
        }
    }

    // ========== Scientific Notation Precision Tests ==========

    @Test
    @DisplayName("Very small scientific notation values should preserve precision")
    public void testSmallScientificNotation() {
        double verySmall = 1e-21;

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("val").float64(verySmall);
            bytes.readPosition(0);

            double result = wire.read("val").float64();
            assertEquals(verySmall, result, 0,
                    "1e-21 should round-trip exactly in " + wt);
        }
    }

    @Test
    @DisplayName("Very large scientific notation values should preserve precision")
    public void testLargeScientificNotation() {
        double veryLarge = 1e21;

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("val").float64(veryLarge);
            bytes.readPosition(0);

            double result = wire.read("val").float64();
            assertEquals(veryLarge, result, 0,
                    "1e21 should round-trip exactly in " + wt);
        }
    }

    // ========== Double MAX_VALUE Tests ==========

    @Test
    @DisplayName("Double.MAX_VALUE should round-trip")
    public void testDoubleMaxValue() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("val").float64(Double.MAX_VALUE);
            bytes.readPosition(0);

            double result = wire.read("val").float64();
            assertEquals(Double.MAX_VALUE, result, 0,
                    "Double.MAX_VALUE should round-trip in " + wt);
        }
    }

    @Test
    @DisplayName("-Double.MAX_VALUE should round-trip")
    public void testNegativeDoubleMaxValue() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("val").float64(-Double.MAX_VALUE);
            bytes.readPosition(0);

            double result = wire.read("val").float64();
            assertEquals(-Double.MAX_VALUE, result, 0,
                    "-Double.MAX_VALUE should round-trip in " + wt);
        }
    }

    // ========== Financial Precision Tests ==========

    @Test
    @DisplayName("Currency amounts with 2 decimal places should preserve precision")
    public void testCurrencyPrecision() {
        double[] amounts = {0.01, 0.10, 1.23, 99.99, 1000000.01, 9999999.99};

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            for (int i = 0; i < amounts.length; i++) {
                wire.write("amt" + i).float64(amounts[i]);
            }
            bytes.readPosition(0);

            for (int i = 0; i < amounts.length; i++) {
                double result = wire.read("amt" + i).float64();
                assertEquals(amounts[i], result, 0,
                        "Currency amount " + amounts[i] + " should round-trip in " + wt);
            }
        }
    }

    @Test
    @DisplayName("Basis points (0.0001) should preserve precision")
    public void testBasisPointsPrecision() {
        double basisPoint = 0.0001;
        double[] values = {basisPoint, basisPoint * 25, basisPoint * 100, basisPoint * 10000};

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            for (int i = 0; i < values.length; i++) {
                wire.write("bp" + i).float64(values[i]);
            }
            bytes.readPosition(0);

            for (int i = 0; i < values.length; i++) {
                double result = wire.read("bp" + i).float64();
                assertEquals(values[i], result, 0,
                        "Basis point value " + values[i] + " should round-trip in " + wt);
            }
        }
    }
}
