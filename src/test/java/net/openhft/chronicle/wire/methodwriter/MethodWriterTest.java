package net.openhft.chronicle.wire.methodwriter;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.UpdateInterceptor;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MethodWriterTest {

    static {
        System.setProperty("dumpCode", "true");
    }

    @Test
    public void allowThrough() {
        check(true);
    }

    @Test
    public void block() {
        check(false);
    }

    public void check(boolean allowThrough) {
        Wire w = WireType.BINARY.apply(Bytes.elasticByteBuffer());
        // checks that no exceptions are thrown here
        UpdateInterceptor ui = (methodName, t) -> allowThrough;
        FundingListener fundingListener = w.methodWriterBuilder(FundingOut.class).updateInterceptor(ui).build();
        fundingListener.funding(new Funding());

        List<String> output = new ArrayList<>();
        @NotNull Consumer<String> consumer = s -> output.add(s);
        FundingListener listener = Mocker.intercepting(FundingListener.class, "", consumer);
        @NotNull MethodReader mr = w.methodReader(listener);

        if (allowThrough) {
            Assert.assertTrue(mr.readOne());
            Assert.assertEquals(1, output.size());
            Assert.assertEquals("[funding[!net.openhft.chronicle.wire.methodwriter.Funding {\n" +
                    "  symbol: 0,\n" +
                    "  fr: NaN,\n" +
                    "  mins: 0\n" +
                    "}\n" +
                    "]]", output.toString());
        } else {
            Assert.assertFalse(mr.readOne());
            Assert.assertEquals(0, output.size());
        }
    }
}
