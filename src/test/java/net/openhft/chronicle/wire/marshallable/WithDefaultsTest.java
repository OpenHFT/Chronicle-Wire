package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

public class WithDefaultsTest extends WireTestCommon {
    @Test
    public void writeMarshallable() {
        doTest(w -> {
        });
        doTest(w -> w.bytes.clear());
        doTest(w -> w.text = "bye");
        doTest(w -> w.flag = false);
        doTest(w -> w.num = 5);
    }

    void doTest(Consumer<WithDefaults> consumer) {
        WithDefaults wd = new WithDefaults();
        consumer.accept(wd);
        String cs = wd.toString();
        WithDefaults o = Marshallable.fromString(cs);
        assertEquals(cs, o.toString());
        assertEquals(wd, o);
    }
}