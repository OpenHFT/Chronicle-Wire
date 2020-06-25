package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.onoes.ExceptionKey;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.Wires;
import org.junit.*;

import java.time.ZoneId;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class NullFieldMarshallingTest {
    protected Map<ExceptionKey, Integer> exceptions;

    @Before
    public void setup() {
        exceptions = Jvm.recordExceptions();
    }

    @After
    public void checkExceptions() {
        // find any discarded resources.
        System.gc();
        Jvm.pause(10);

        if (Jvm.hasException(exceptions)) {
            Jvm.dumpException(exceptions);
            Jvm.resetExceptionHandlers();
            Assert.fail();
        }
    }

    @Test
    public void testAbstractNullFieldUnmarshalledCorrectlyText() {
        VO object = new VO();

        String val = Marshallable.$toString(object);

        VO object2 = Marshallable.fromString(val);
        assertNotNull(object2);
        assertNull(object2.zoneId);
    }

    @Ignore("https://github.com/OpenHFT/Chronicle-Wire/issues/165")
    @Test
    public void testAbstractNullFieldUnmarshalledCorrectlyBinary() {
        VO object = new VO();
        final Wire wire = Wires.acquireBinaryWire();
        wire.write().typedMarshallable(object);

        VO object2 = wire.read().typedMarshallable();
        assertNotNull(object2);
        assertNull(object2.zoneId);
    }

    static class VO extends SelfDescribingMarshallable {
        ZoneId zoneId;
    }
}
