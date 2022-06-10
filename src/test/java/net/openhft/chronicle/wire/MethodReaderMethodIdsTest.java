package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class MethodReaderMethodIdsTest extends WireTestCommon {

    @Test
    public void shouldDetermineMethodNamesFromMethodIds() {
        final BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        wire.usePadding(true);

        final Speaker speaker = wire.methodWriterBuilder(Speaker.class).get();
        assertFalse("check we are using generated code", Proxy.isProxyClass(speaker.getClass()));
        speaker.say("hello");

        final AtomicInteger heard = new AtomicInteger();
        final MethodReader reader = new VanillaMethodReaderBuilder(wire).build((Speaker) message -> heard.incrementAndGet());
        assertFalse("check we are using generated code", reader instanceof VanillaMethodReader);
        assertTrue(reader.readOne());
        assertEquals(1, heard.get());
    }

    interface Speaker {
        @MethodId(7)
        void say(final String message);
    }
}