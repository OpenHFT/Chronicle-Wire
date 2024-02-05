package run.chronicle.account.dto;

import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.converter.NanoTime;
import net.openhft.chronicle.wire.converter.ShortText;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static run.chronicle.account.dto.TransferTest.getTransfer;

public class TransferOKTest {
    // Expected string representation of TransferOK object
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

    // Test for the string representation of TransferOK object
    @Test
    public void testToString() {
        // Create a TransferOK object and set its properties then asserting
        assertEquals(EXPECTED,
                new TransferOK()
                        .target(ShortText.INSTANCE.parse("sender"))
                        .sender(ShortText.INSTANCE.parse("target"))
                        .sendingTime(NanoTime.INSTANCE.parse("2001/02/03T04:05:06.777888999"))
                        .transfer(getTransfer()) // Assuming getTransfer() method is defined and returns a Transfer object
                        .toString());
    }

    // Test for creating a TransferOK object from a string representation
    @Test
    public void testFromString() {
        // Create a TransferOK object from the EXPECTED string
        TransferOK tok = Marshallable.fromString(EXPECTED);

        // Assert that the transfer field of the created object matches the transfer returned by getTransfer()
        assertEquals(TransferTest.getTransfer(), tok.transfer()); // Assuming TransferTest.getTransfer() method is defined
    }
}
