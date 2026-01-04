/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests handling of escaped characters across various Wire output formats.
 */
class EscapeCharsTest extends WireTestCommon {

    // Override the threadDump from WireTestCommon to use the parent implementation
    @Override
    @BeforeEach
    void threadDump() {
        super.threadDump();
    }

    /**
     * Combinations of data to be used for testing.
     *
     * @return Collection of test data
     */
    @NotNull
    public static Collection<Object[]> combinations() {
        @NotNull List<Object[]> list = new ArrayList<>();

        // Depending on debug mode, select appropriate executor
        ExecutorService es = Jvm.isDebug() ? Executors.newSingleThreadExecutor() : ForkJoinPool.commonPool();
        for (char i = 0; i <= 300; i += 4) {
            if (i == 300)
                i = 0x2028;
            char ch1 = (char) (i + 1);
            char ch2 = (char) (i + 2);
            char ch3 = (char) (i + 3);

            // Instantiate various Wire types
            Wire json = WireType.JSON_ONLY.apply(Bytes.allocateElasticDirect());
            Wire text = WireType.TEXT.apply(Bytes.allocateElasticDirect());
            Wire yaml = WireType.YAML_ONLY.apply(Bytes.allocateElasticDirect());

            String str = "" + i + ch1 + ch2 + ch3;
            String desc = "(" + (int) i + ") " + str;

            // Add test data for different Wire types
            list.add(new Object[]{"JSON " + desc, es.submit(() -> testEscaped(str, json))});
            list.add(new Object[]{"TEXT " + desc, es.submit(() -> testEscaped(str, text))});
            list.add(new Object[]{"YAML " + desc, es.submit(() -> testEscaped(str, yaml))});
        }
        return list;
    }

    /**
     * Utility to test how escaped characters are handled by the provided Wire.
     *
     * @param str  String to be tested
     * @param wire Wire format for the test
     */
    private static void testEscaped(String str, @NotNull Wire wire) {
        wire.reset();

        // Write the string to wire
        wire.write(str)
                .text(str);
        String str2 = str.substring(2) + str.substring(0, 2);
        wire.write(str2)
                .text(str2);

        @NotNull StringBuilder sb = new StringBuilder();

        // Read from wire and validate
        @Nullable String s = wire.read(sb).text();
        assertEquals(str, sb.toString(),
                "first key should match input string " + str);
        assertEquals(str, s,
                "first value should match input string " + str);
        @Nullable String ss = wire.read(sb).text();
        assertEquals(str2, sb.toString(),
                "second key should match input string " + str2);
        assertEquals(str2, ss,
                "second value should match input string " + str2);
    }

    /**
     * Test to ensure escaped characters are handled properly.
     */
    @MethodSource("combinations")
    @ParameterizedTest(name = "{0}")
    @DisplayName("Handles escaped characters across wire types")
    void testEscaped(@NotNull String chs, Future<?> future) throws ExecutionException, InterruptedException {
        assertNull(future.get(),
                "escape character task should complete without exception for " + chs);
    }
}
