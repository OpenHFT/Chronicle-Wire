package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import org.junit.After;
import org.junit.Before;

public class WireTestCommon {

    @Before
    public void enableReferenceTracing() {
        AbstractReferenceCounted.enableReferenceTracing();
    }

    @After
    public void assertReferencesReleased() {
        AbstractReferenceCounted.assertReferencesReleased();
    }
}
