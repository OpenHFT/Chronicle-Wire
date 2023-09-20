/*
 * Copyright 2016-2020 chronicle.software
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
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests for handling escaped characters in various Wire formats.
 */
@RunWith(value = Parameterized.class)
public class EscapeCharsTest extends WireTestCommon {
    @NotNull
    final String chs;
    private final Future future;

    // Override the threadDump from WireTestCommon to use the parent implementation
    @Override
    @Before
    public void threadDump() {
        super.threadDump();
    }

    /**
     * Constructor for parameterized test.
     *
     * @param chs   Characters to test
     * @param future Represents the result of an asynchronous computation
     */
    public EscapeCharsTest(@NotNull String chs, Future future) {
        this.chs = chs;
        this.future = future;
    }

    /**
     * Combinations of data to be used for testing.
     *
     * @return Collection of test data
     */
    @NotNull
    @Parameterized.Parameters(name = "{0}")
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
    static void testEscaped(String str, @NotNull Wire wire) {
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
        assertEquals("key " + str, str, sb.toString());
        assertEquals("value " + str, str, s);
        @Nullable String ss = wire.read(sb).text();
        assertEquals("key " + str2, str2, sb.toString());
        assertEquals("value " + str2, str2, ss);
    }

    /**
     * Test to ensure escaped characters are handled properly.
     */
    @Test
    public void testEscaped() throws ExecutionException, InterruptedException {
        assertNull(future.get());
    }
}
