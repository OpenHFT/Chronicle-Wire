package run.chronicle.account.dto;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.converter.NanoTime;
import net.openhft.chronicle.wire.converter.ShortText;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TransferTest {
    // Factory method to create a pre-configured Transfer object
    static Transfer getTransfer() {
        return new Transfer()
                .sender(ShortText.INSTANCE.parse("sender"))
                .target(ShortText.INSTANCE.parse("target"))
                .sendingTime(NanoTime.INSTANCE.parse("2001/02/03T04:05:06.007008009"))
                .amount(1)
                .currency((int) ShortText.INSTANCE.parse("CURR"))
                .from(12345)
                .to(67890)
                .reference(Bytes.from("reference"));
    }

    // Test to validate the string representation of a Transfer object
    @Test
    public void testToString() {
        // Expected string representation of Transfer object and assert
        assertEquals("" +
                        "!run.chronicle.account.dto.Transfer {\n" +
                        "  sender: sender,\n" +
                        "  target: target,\n" +
                        "  sendingTime: 2001-02-03T04:05:06.007008009,\n" +
                        "  from: 12345,\n" +
                        "  to: 67890,\n" +
                        "  currency: CURR,\n" +
                        "  amount: 1.0,\n" +
                        "  reference: reference\n" +
                        "}\n",
                getTransfer().toString());
    }
}
