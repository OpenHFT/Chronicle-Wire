package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import org.junit.After;
import org.junit.Before;

public class WireTestCommon {

    private boolean gt;

    @Before
    public void enableReferenceTracing() {
        AbstractReferenceCounted.enableReferenceTracing();
    }

    @After
    public void assertReferencesReleased() {
        AbstractReferenceCounted.assertReferencesReleased();
    }

    @Before
    public void rememberGenerateTuples() {
        gt = Wires.GENERATE_TUPLES;
    }

    @After
    public void restoreGenerateTuples() {
        Wires.GENERATE_TUPLES = gt;
    }
}
