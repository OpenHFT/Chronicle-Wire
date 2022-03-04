package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class VanillaWireParserTest extends WireTestCommon {

    private static Listener impl() {
        return m -> {
        };
    }

    @Test
    public void shouldDetermineMethodNamesFromMethodIds() {
        final BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        wire.usePadding(true);

        final Speaker speaker =
                wire.methodWriterBuilder(Speaker.class).get();
        speaker.say("hello");

        final MethodReader reader = new VanillaMethodReaderBuilder(wire).build(impl());
        assertTrue(reader.readOne());
    }

    interface Speaker {
        @MethodId(7)
        void say(final String message);
    }

    interface Listener {
        void hear(final String message);
    }
}