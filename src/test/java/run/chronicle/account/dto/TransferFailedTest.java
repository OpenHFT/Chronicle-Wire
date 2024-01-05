package run.chronicle.account.dto;

import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.converter.Base85;
import net.openhft.chronicle.wire.converter.NanoTime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TransferFailedTest {

    // Expected string representation of a TransferFailed object
    public static final String EXPECTED = "" +
            "!run.chronicle.account.dto.TransferFailed {\n" +
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
            "  },\n" +
            "  reason: reasons\n" +
            "}\n";

    // Test to check if the toString method of TransferFailed class generates the expected string
    @Test
    public void testToString() {
        assertEquals(EXPECTED,
                new TransferFailed()
                        .target(Base85.INSTANCE.parse("sender")) // setting the target
                        .sender(Base85.INSTANCE.parse("target")) // setting the sender
                        .sendingTime(NanoTime.INSTANCE.parse("2001/02/03T04:05:06.777888999")) // setting the sendingTime
                        .reason("reasons") // setting the reason for failure
                        .transfer(TransferTest.getTransfer()) // setting the transfer details
                        .toString()); // converting to string and comparing with the expected string
    }

    // Test to validate if the TransferFailed object is correctly created from a string representation
    @Test
    public void testFromString() {
        TransferFailed tf = Marshallable.fromString(EXPECTED); // creating an object from the string representation
        assertEquals(TransferTest.getTransfer(), tf.transfer()); // comparing the transfer details
        assertEquals("reasons", tf.reason()); // comparing the failure reason
    }
}
