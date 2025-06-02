/*
 * Copyright 2016-2022 chronicle.software
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
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;

@Deprecated(/* Should be fully covered by YamlSpecificationTest */)
public class YamlSpecTest extends WireTestCommon {
    static String DIR = "/yaml/spec/";

    public static void doTest(String file, String expected) {
        Bytes<?> b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + file);

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals(expected, actual);

        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void test2_18Multi_lineFlowScalarsFixed() {
        Bytes<?> b = Bytes.elasticByteBuffer();
        try {
            InputStream is = YamlSpecTest.class.getResourceAsStream
                    (DIR + "2_18Multi_lineFlowScalarsFixed.yaml");

            Object o = Marshallable.fromString(is);
            Assert.assertNotNull(o);
            String actual = o.toString();
            Assert.assertEquals("{plain=\n" +
                    "  This unquoted scalar\n" +
                    "  spans many lines., quoted=So does this\n" +
                    "  quoted scalar.\n" +
                    "}", actual.replaceAll("\r", ""));

        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void test2_21MiscellaneousFixed() {
        doTest("2_21MiscellaneousFixed.yaml", "{null=, booleans=[true, false], string=012345}");
    }
}
