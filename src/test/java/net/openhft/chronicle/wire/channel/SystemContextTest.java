package net.openhft.chronicle.wire.channel;

import org.junit.Test;

import static net.openhft.chronicle.wire.channel.SystemContext.INSTANCE;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class SystemContextTest {

    @Test
    public void availableProcessors() {
        assertTrue(INSTANCE.availableProcessors() > 0);
    }

    @Test
    public void hostId() {
        assertTrue(INSTANCE.hostId() >= 0);
    }

    @Test
    public void hostName() {
        assertNotEquals("localhost", INSTANCE.hostName());
        assertTrue(INSTANCE.hostName(), INSTANCE.hostName().matches("[a-zA-Z].*"));
    }

    @Test
    public void upTime() {
        assertTrue(INSTANCE.upTime() > 1.6e18);
        assertTrue(INSTANCE.upTime() < 2e18);
    }

    @Test
    public void userCountry() {
        assertTrue(INSTANCE.userCountry().matches("[a-zA-Z].*"));
    }

    @Test
    public void userName() {
        assertTrue(INSTANCE.userName().matches("[a-zA-Z].*"));
    }
}