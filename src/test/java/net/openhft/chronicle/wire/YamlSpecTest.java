/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@Deprecated(/* Should be fully covered by YamlSpecificationTest */)
class YamlSpecTest extends WireTestCommon {
    private static String DIR = "/yaml/spec/";

    private static void doTest(String file, String expected) {
        Bytes<?> b = Bytes.allocateElasticOnHeap();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + file);

            Object o = Marshallable.fromString(is);
            assertNotNull(o);
            String actual = o.toString();
            assertEquals(expected, actual);

        } finally {
            b.releaseLast();
        }
    }

    @Test
    void test2_18Multi_lineFlowScalarsFixed() {
        Bytes<?> b = Bytes.allocateElasticOnHeap();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_18Multi_lineFlowScalarsFixed.yaml");

            Object o = Marshallable.fromString(is);
            assertNotNull(o);
            String actual = o.toString();
            assertEquals("{plain=\n" +
                    "  This unquoted scalar\n" +
                    "  spans many lines., quoted=So does this\n" +
                    "  quoted scalar.\n" +
                    "}", actual.replaceAll("\r", ""));

        } finally {
            b.releaseLast();
        }
    }

    @Test
    void test2_21MiscellaneousFixed() {
        doTest("2_21MiscellaneousFixed.yaml", "{null=, booleans=[true, false], string=012345}");
    }
}
