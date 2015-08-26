package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

/**
 * @author Rob Austin.
 */
public class BinaryToTextTest {

    @Test
    public void test() throws Exception {
        Bytes tbytes = Bytes.elasticByteBuffer();
        Wire tw = new BinaryWire(tbytes);
        tw.writeDocument(false, w->w.write(() -> "key").text("hello"));
        System.out.println(Wires.fromSizePrefixedBlobs(tbytes));
    }

}
