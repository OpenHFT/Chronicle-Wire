package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

/**
 * @author Rob Austin.
 */
public class DefaultZeroLicenceTest {

    @Test(expected = IllegalStateException.class)
    public void testLicenceCheck() throws Exception {

        try {
            WireType.DEFAULT_ZERO_BINARY.apply(Bytes.elasticByteBuffer());
        } catch (Exception e) {
            e.getMessage().contains("A Chronicle Wire Enterprise licence is required to run this " +
                    "code");
            throw e;
        }

    }
}
