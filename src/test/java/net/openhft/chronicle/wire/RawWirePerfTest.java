package net.openhft.chronicle.wire;

import org.junit.Test;

/**
 * Created by peter.lawrey on 19/01/15.
 */
public class RawWirePerfTest {
    @Test
    public void testRawPerf() {
        BinaryWirePerfTest test = new BinaryWirePerfTest(-1, true, false, true);
//        test.wirePerf();
        test.wirePerfInts();
    }
}
