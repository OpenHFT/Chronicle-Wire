package run.chronicle.account.dto;

import net.openhft.chronicle.wire.converter.NanoTime;
import net.openhft.chronicle.wire.converter.ShortText;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AccountStatusTest {
    @Test
    public void testToString() {
        assertEquals("" +
                        "!run.chronicle.account.dto.AccountStatus {\n" +
                        "  sender: sender,\n" +
                        "  target: target,\n" +
                        "  sendingTime: 2001-02-03T04:05:06.007008009,\n" +
                        "  name: name,\n" +
                        "  account: 2,\n" +
                        "  currency: CURR,\n" +
                        "  amount: 1.0\n" +
                        "}\n",
                getAccountStatus().toString());
    }

    static AccountStatus getAccountStatus() {
        return new AccountStatus()
                .sender(ShortText.INSTANCE.parse("sender"))
                .target(ShortText.INSTANCE.parse("target"))
                .sendingTime(NanoTime.INSTANCE.parse("2001/02/03T04:05:06.007008009"))
                .amount(1)
                .account(2)
                .currency((int) ShortText.INSTANCE.parse("CURR"))
                .name("name");
    }
}
