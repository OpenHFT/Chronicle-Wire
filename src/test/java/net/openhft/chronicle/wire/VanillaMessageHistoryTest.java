package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.ClassLookup;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class VanillaMessageHistoryTest {
    @Test
    public void equalsHashCode() {
        VanillaMessageHistory vmh = new VanillaMessageHistory();
        vmh.addSource(1, 128);
        vmh.addTiming(12121212);
        final ClassLookup classLookup = ClassAliasPool.CLASS_ALIASES.wrap();
        classLookup.addAlias(VanillaMessageHistory.class, "VMH");
        BinaryWire wire = new BinaryWire(new HexDumpBytes());
        wire.classLookup(classLookup);
        wire.write("vmh").object(vmh);
        assertEquals("" +
                        "c3 76 6d 68                                     # vmh:\n" +
                        "b6 03 56 4d 48                                  # VMH\n" +
                        "81 33 00                                        # VanillaMessageHistory\n" +
                        "c7 73 6f 75 72 63 65 73                         # sources:\n" +
                        "82 0b 00 00 00                                  # sequence\n" +
                        "                                                # source id & index\n" +
                        "a1 01 af 80 00 00 00 00 00 00 00                # 1\n" +
                        "c7 74 69 6d 69 6e 67 73                         # timings:\n" +
                        "82 0e 00 00 00                                  # sequence\n" +
                        "                                                # timing in nanos\n" +
                        "a6 7c f4 b8 00                                  # 12121212\n" +
                        "a7 timestamp\n",
                wire.bytes().toHexString().replaceAll("\na7.*\n", "\na7 timestamp\n"));

        VanillaMessageHistory vmh2 = new VanillaMessageHistory();
        VanillaMessageHistory vmh3 = new VanillaMessageHistory();
        assertEquals(vmh3.hashCode(),
                vmh2.hashCode());
        Object o = wire.read("vmh").object(vmh2, VanillaMessageHistory.class);
        assertNotNull(o);
        // add the last timing which is added on read
        vmh.addTiming(vmh2.timing(1));
        assertEquals(vmh.toString(), vmh2.toString());
        assertEquals(vmh, vmh2);
        assertEquals(vmh.hashCode(),
                vmh2.hashCode());
    }

}