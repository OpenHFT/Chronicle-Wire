/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for the WithDefaults functionality.
 * Extends the WireTestCommon to leverage utilities related to wire tests.
 */
class WithDefaultsTest extends WireTestCommon {

    /**
     * Tests the writeMarshallable functionality of WithDefaults under various scenarios.
     */
    @Test
    @DisplayName("WithDefaults round-trips across mutation scenarios")
    void writeMarshallable() {
        for (Scenario scenario : new Scenario[]{
                new Scenario("default", w -> {
                }),
                new Scenario("clear-bytes", w -> w.bytes.clear()),
                new Scenario("text", w -> w.text = "bye"),
                new Scenario("flag", w -> w.flag = false),
                new Scenario("num", w -> w.num = 5),
        }) {
            RoundTripResult result = roundTrip(scenario.mutation);
            assertEquals(result.serialised, result.roundTripped.toString(),
                    "Serialised output should match round-tripped form for " + scenario.name);
            assertEquals(result.original, result.roundTripped,
                    "Round-tripped object should equal original for " + scenario.name);
            assertEquals(result.original.text, result.roundTripped.text,
                    "text should round-trip for " + scenario.name);
            assertEquals(result.original.flag, result.roundTripped.flag,
                    "flag should round-trip for " + scenario.name);
            assertEquals(result.original.num, result.roundTripped.num,
                    "num should round-trip for " + scenario.name);
            assertEquals(result.original.num2, result.roundTripped.num2,
                    "num2 should round-trip for " + scenario.name);
            assertEquals(result.original.qty, result.roundTripped.qty, 0.0,
                    "qty should round-trip for " + scenario.name);
            assertEquals(result.original.bytes.toString(), result.roundTripped.bytes.toString(),
                    "bytes should round-trip for " + scenario.name);
        }
    }

    private static final class Scenario {
        private final String name;
        private final Consumer<WithDefaults> mutation;

        private Scenario(String name, Consumer<WithDefaults> mutation) {
            this.name = name;
            this.mutation = mutation;
        }
    }

    private static final class RoundTripResult {
        private final WithDefaults original;
        private final WithDefaults roundTripped;
        private final String serialised;

        private RoundTripResult(WithDefaults original, WithDefaults roundTripped, String serialised) {
            this.original = original;
            this.roundTripped = roundTripped;
            this.serialised = serialised;
        }
    }

    /**
     * Utility function to perform tests on WithDefaults.
     * Initializes an instance, applies the consumer action, and then validates the string
     * representation and object equality.
     *
     * @param consumer Consumer action to apply on the WithDefaults instance.
     */
    private RoundTripResult roundTrip(Consumer<WithDefaults> consumer) {
        // Initialize the WithDefaults instance
        WithDefaults wd = new WithDefaults();

        // Apply the consumer action on the instance
        consumer.accept(wd);

        // Convert the instance to its string representation
        String cs = wd.toString();

        // Convert the string representation back to a WithDefaults object
        WithDefaults o = Marshallable.fromString(cs);
        return new RoundTripResult(wd, o, cs);
    }
}
