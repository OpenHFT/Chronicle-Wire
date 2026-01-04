/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

@Deprecated(/* Should be fully covered by YamlSpecificationTest */)
public class YamlSpecTest extends WireTestCommon {
    private static final String DIR = "/yaml/spec/";

    private static boolean doTest(String file, String expected) {
        Bytes<?> b = Bytes.allocateElasticOnHeap();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream(DIR + file);

            Object o = Marshallable.fromString(is);
            Assertions.assertNotNull(o, "Spec input should parse for file: " + file);
            String actual = o.toString();
            Assertions.assertEquals(expected, actual, "Spec output should match expected for file: " + file);
            return true;

        } finally {
            b.releaseLast();
        }
    }

    @Test
    @DisplayName("YAML spec 2.18 flow scalars parse")
    public void test2_18Multi_lineFlowScalarsFixed() {
        Bytes<?> b = Bytes.allocateElasticOnHeap();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream(DIR + "2_18Multi_lineFlowScalarsFixed.yaml");

            Object o = Marshallable.fromString(is);
            Assertions.assertNotNull(o, "Spec 2.18 should parse into object");
            String actual = o.toString();
            Assertions.assertEquals("{plain=\n" +
                    "  This unquoted scalar\n" +
                    "  spans many lines., quoted=So does this\n" +
                    "  quoted scalar.\n" +
                    "}", actual.replaceAll("\r", ""),
                    "Spec 2.18 output should match expected rendering");

        } finally {
            b.releaseLast();
        }
    }

    @Test
    @DisplayName("YAML spec 2.21 miscellaneous parse")
    public void test2_21MiscellaneousFixed() {
        Assertions.assertTrue(doTest("2_21MiscellaneousFixed.yaml", "{null=, booleans=[true, false], string=012345}"),
                "YAML spec 2.21 should match expected output");
    }
}
