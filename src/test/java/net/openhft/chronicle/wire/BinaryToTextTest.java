package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Rob Austin.
 */
public class BinaryToTextTest {

    @Ignore("JIRA https://higherfrequencytrading.atlassian.net/browse/WIRE-30")
    @Test
    public void test() throws Exception {
        Bytes tbytes = Bytes.elasticByteBuffer();
        Wire tw = new BinaryWire(tbytes);
        tw.write(() -> "key").text("hello");
        System.out.println(Wires.fromSizePrefixedBinaryToText(tbytes));
    }

}
