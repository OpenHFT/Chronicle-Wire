package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WireInternalTest {

    @Test
    public void testFromSizePrefixedBinaryToText() throws Exception {
        Bytes bytes = Bytes.elasticByteBuffer();
        Wire out = new BinaryWire(bytes);
        out.writeDocument(true, w -> w
                .write(() -> "csp").text("csp://hello-world")
                .write(() -> "tid").int64(123456789));
        out.writeDocument(false, w -> w.write(() -> "reply").marshallable(
                w2 -> w2.write(() -> "key").int16(1)
                        .write(() -> "value").text("Hello World")));
        out.writeDocument(false, w -> w.write(() -> "reply").sequence(
                w2 -> {
                    w2.text("key");
                    w2.int16(2);
                    w2.text("value");
                    w2.text("Hello World2");
                }));
        out.writeDocument(false, wireOut -> wireOut.writeEventName(() -> "userid").text("peter"));

        String actual = WireInternal.fromSizePrefixedBinaryToText(bytes);
        assertEquals("--- !!meta-data #binary\n" +
                "csp: \"csp://hello-world\"\n" +
                "tid: 123456789\n" +
                "# position: 35\n" +
                "--- !!data #binary\n" +
                "reply: {\n" +
                "  key: 1,\n" +
                "  value: Hello World\n" +
                "}\n" +
                "# position: 73\n" +
                "--- !!data #binary\n" +
                "reply: [\n" +
                "  key,\n" +
                "  2,\n" +
                "  value,\n" +
                "  Hello World2\n" +
                "]\n" +
                "# position: 112\n" +
                "--- !!data #binary\n" +
                "userid: peter\n", actual);
    }
}