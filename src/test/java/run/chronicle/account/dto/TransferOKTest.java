package run.chronicle.account.dto;

import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.converter.Base85;
import net.openhft.chronicle.wire.converter.NanoTime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static run.chronicle.account.dto.TransferTest.getTransfer;

public class TransferOKTest {

    public static final String EXPECTED = "" +
            "!run.chronicle.account.dto.TransferOK {\n" +
            "  sender: target,\n" +
            "  target: sender,\n" +
            "  sendingTime: 2001-02-03T04:05:06.777888999,\n" +
            "  transfer: {\n" +
            "    sender: sender,\n" +
            "    target: target,\n" +
            "    sendingTime: 2001-02-03T04:05:06.007008009,\n" +
            "    from: 12345,\n" +
            "    to: 67890,\n" +
            "    currency: CURR,\n" +
            "    amount: 1.0,\n" +
            "    reference: reference\n" +
            "  }\n" +
            "}\n";

    @Test
    public void testToString() {
        assertEquals(EXPECTED,
                new TransferOK()
                        .target(Base85.INSTANCE.parse("sender"))
                        .sender(Base85.INSTANCE.parse("target"))
                        .sendingTime(NanoTime.INSTANCE.parse("2001/02/03T04:05:06.777888999"))
                        .transfer(getTransfer())
                        .toString());
    }

    @Test
    public void testFromString() {
        TransferOK tok = Marshallable.fromString(EXPECTED);
        assertEquals(TransferTest.getTransfer(), tok.transfer());
    }
}
