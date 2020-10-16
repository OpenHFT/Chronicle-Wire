package net.openhft.chronicle.wire;


import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import org.junit.Assert;
import org.junit.Test;

public class WireResetTest {
    @Test
    public void test() throws Exception {
        Event event = new Event();
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
}