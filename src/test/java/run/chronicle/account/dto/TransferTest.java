package run.chronicle.account.dto;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.converter.Base85;
import net.openhft.chronicle.wire.converter.NanoTime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TransferTest {
    // Factory method to create a pre-configured Transfer object
    static Transfer getTransfer() {
        return new Transfer()
                .sender(Base85.INSTANCE.parse("sender")) // Set sender using Base85 encoding
                .target(Base85.INSTANCE.parse("target")) // Set target using Base85 encoding
                .sendingTime(NanoTime.INSTANCE.parse("2001/02/03T04:05:06.007008009")) // Set sending time
                .amount(1) // Set amount
                .currency((int) Base85.INSTANCE.parse("CURR")) // Set currency using Base85 encoding
                .from(12345) // Set source account ID
                .to(67890) // Set destination account ID
                .reference(Bytes.from("reference")); // Set reference as a bytes object
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
