package net.openhft.chronicle.wire.methodwriter;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;

public class MethodWriterTest {

    static {
        System.setProperty("dumpCode", "true");
    }

    @Test
    public void test() {
        Wire w = WireType.BINARY.apply(Bytes.elasticByteBuffer());
        // checks that no expections are thrown here
        FundingListener fundingListener = w.methodWriter(FundingOut.class);
        fundingListener.funding(new Funding());
    }
}
