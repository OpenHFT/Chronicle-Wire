package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Rob Austin.
 */
public class ReadingPeekYamlTest {


    @Test
    public void test() {
        Bytes b = Bytes.elasticByteBuffer();
        BinaryWire wire = (BinaryWire) WireType.BINARY.apply(b);
        Assert.assertEquals("", wire.readingPeekYaml());
        try (DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("some-data").marshallable(m -> {
                m.write("some-other-data").int64(0);
                Assert.assertEquals("", wire.readingPeekYaml());
            });
        }

        try (DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("some-new").marshallable(m -> {
                m.write("some-other--new-data").int64(0);
                Assert.assertEquals("", wire.readingPeekYaml());
            });
        }
        Assert.assertEquals("", wire.readingPeekYaml());

        try (DocumentContext dc = wire.readingDocument()) {
            Assert.assertEquals("--- !!data #binary\n" +
                    "some-data: {\n" +
                    "  some-other-data: 0\n" +
                    "}\n", wire.readingPeekYaml());
            dc.wire().read("some-data");
            Assert.assertEquals("--- !!data #binary\n" +
                    "some-data: {\n" +
                    "  some-other-data: 0\n" +
                    "}\n", wire.readingPeekYaml());

        }
        Assert.assertEquals("", wire.readingPeekYaml());


        try (DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("some-data").marshallable(m -> {
                m.write("some-other-data").int64(0);
                Assert.assertEquals("", wire.readingPeekYaml());
            });
        }

        try (DocumentContext dc = wire.readingDocument()) {
            Assert.assertEquals("# position: 36, header: 0\n" +
                    "--- !!data #binary\n" +
                    "some-new: {\n" +
                    "  some-other--new-data: 0\n" +
                    "}\n", wire.readingPeekYaml());
            dc.wire().read("some-data");
            Assert.assertEquals("# position: 36, header: 0\n" +
                    "--- !!data #binary\n" +
                    "some-new: {\n" +
                    "  some-other--new-data: 0\n" +
                    "}\n", wire.readingPeekYaml());

        }
    }


}
