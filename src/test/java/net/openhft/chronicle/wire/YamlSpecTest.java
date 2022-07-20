package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;

@SuppressWarnings("rawtypes")
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
