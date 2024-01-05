package run.chronicle.account.dto;

import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.converter.Base85;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static run.chronicle.account.dto.AccountStatusTest.getAccountStatus;

public class AccountStatusOKTest {
    @Test
    public void testFromString() {
        AccountStatusOK asf = Marshallable.fromString(""+
                "!run.chronicle.account.dto.AccountStatusOK {\n" +
                "  sender: sender,\n" +
                "  target: target,\n" +
                "  sendingTime: 2001/02/03T04:05:06.007008009,\n" +
                "  accountStatus: {\n" +
                "    sender: sender,\n" +
                "    target: target,\n" +
                "    sendingTime: 2001-02-03T04:05:06.007008009,\n" +
                "    name: name,\n" +
                "    account: 2,\n" +
                "    currency: CURR,\n" +
                "    amount: 1.0" +
                "  }\n" +
                "}");
        assertEquals("sender", Base85.INSTANCE.asString(asf.sender()));
        assertEquals("target", Base85.INSTANCE.asString(asf.target()));
        assertEquals(getAccountStatus(), asf.accountStatus());
    }

}
