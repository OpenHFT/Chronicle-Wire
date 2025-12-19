/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for the WithDefaults functionality.
 * Extends the WireTestCommon to leverage utilities related to wire tests.
 */
public class WithDefaultsTest extends WireTestCommon {

    /**
     * Tests the writeMarshallable functionality of WithDefaults under various scenarios.
     */
    @Test
    public void writeMarshallable() {
        for (Scenario scenario : new Scenario[]{
                new Scenario("default", w -> {
                }),
                new Scenario("clear-bytes", w -> w.bytes.clear()),
                new Scenario("text", w -> w.text = "bye"),
                new Scenario("flag", w -> w.flag = false),
                new Scenario("num", w -> w.num = 5),
        }) {
            RoundTripResult result = roundTrip(scenario.mutation);
            assertEquals(result.serialised, result.roundTripped.toString(), "withDefaults: serialised (" + scenario.name + ")");
            assertEquals(result.original, result.roundTripped, "withDefaults: object (" + scenario.name + ")");
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
