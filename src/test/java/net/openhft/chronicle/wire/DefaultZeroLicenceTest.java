package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Rob Austin.
 */
public class DefaultZeroLicenceTest {
    @Test
    public void testLicenceCheck() {
        try {
            WireType.DEFAULT_ZERO_BINARY.apply(Bytes.elasticByteBuffer());
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains(
                    "A Chronicle Wire Enterprise licence is required to run this code"));
        }
    }
}
