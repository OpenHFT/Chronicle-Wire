package run.chronicle.account.dto;

import net.openhft.chronicle.wire.converter.Base85;
import net.openhft.chronicle.wire.converter.NanoTime;
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
                        "  amount: 1\n" +
                        "}\n",
                getAccountStatus().toString());
    }

    static AccountStatus getAccountStatus() {
        return new AccountStatus()
                .sender(Base85.INSTANCE.parse("sender"))
                .target(Base85.INSTANCE.parse("target"))
                .sendingTime(NanoTime.INSTANCE.parse("2001/02/03T04:05:06.007008009"))
                .amount(1)
                .account(2)
                .currency((int) Base85.INSTANCE.parse("CURR"))
                .name("name");
    }
}
