package net.openhft.chronicle.wire;

import org.junit.Test;

import java.io.StreamCorruptedException;

/**
 * Created by peter on 19/01/15.
 */
public class RawWirePerfTest {
    @Test
    public void testRawPerf() throws StreamCorruptedException {
        BinaryWirePerfTest test = new BinaryWirePerfTest(-1, true, false, true);
//        test.wirePerf();
        test.wirePerfInts();
    }
}
