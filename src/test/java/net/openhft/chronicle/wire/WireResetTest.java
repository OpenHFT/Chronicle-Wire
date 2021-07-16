package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.AbstractCloseable;
import net.openhft.chronicle.core.io.Closeable;
import org.junit.Assert;
import org.junit.Test;

// see https://github.com/OpenHFT/Chronicle-Wire/issues/225
public class WireResetTest extends WireTestCommon {
    @Test
    public void test() {
        Event event = new Event();
        Assert.assertFalse(event.isClosed());

        event.reset();
        Assert.assertFalse(event.isClosed());
    }

    @Test
    public void testEventAbstractCloseable() {
        EventAbstractCloseable event = new EventAbstractCloseable();
        Assert.assertFalse(event.isClosed());

        event.reset();
        Assert.assertFalse(event.isClosed());
    }

    public static class Event extends SelfDescribingMarshallable implements Closeable {
        private boolean isClosed;

        //other fields

        @Override
        public void close() {
            isClosed = true;
        }

        @Override
        public boolean isClosed() {
            return isClosed;
        }
    }

    public static class EventAbstractCloseable extends AbstractCloseable implements Marshallable {
        @Override
        protected void performClose() {

        }
    }
}