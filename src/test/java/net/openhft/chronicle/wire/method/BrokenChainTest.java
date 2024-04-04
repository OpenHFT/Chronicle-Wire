package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class BrokenChainTest extends WireTestCommon {
    interface First {
        Second pre(String pre);
    }

    interface Second {
        void msg(String msg);
    }

    @Test
    public void brokenChainYaml() {
        doBrokenChain(WireType.YAML_ONLY);
    }

    @Test
    public void brokenChainText() {
        doBrokenChain(WireType.TEXT);
    }

    @Test
    public void brokenChainBinary() {
        doBrokenChain(WireType.BINARY_LIGHT);
    }

    private void doBrokenChain(WireType wireType) {
        Bytes<byte[]> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = wireType.apply(bytes);
        First writer = wire.methodWriter(First.class);
        assertTrue(wire.writingIsComplete());

        List<String> list = new ArrayList<>();
        First first = pre -> msg -> list.add("pre: " + pre + ", msg: " + msg);
        MethodReader reader = wire.methodReader(first);

        assertFalse(reader.readOne());

        wire.rollbackIfNotComplete();

        assertFalse(reader.readOne());

        Second second = writer.pre("pre");
        assertFalse(wire.writingIsComplete());
        second.msg("msg");
        assertTrue(wire.writingIsComplete());
        wire.rollbackIfNotComplete();

        assertTrue(reader.readOne());
        assertFalse(reader.readOne());
        assertEquals("[pre: pre, msg: msg]", list.toString());

        list.clear();
        Second secondB = writer.pre("bad-pre");
        assertFalse(wire.writingIsComplete());
        wire.rollbackIfNotComplete();
        assertTrue(wire.writingIsComplete());
        assertFalse(reader.readOne());
        assertEquals("[]", list.toString());

        Second secondC = writer.pre("pre-C");
        assertFalse(wire.writingIsComplete());
        secondC.msg("msg-C");
        assertTrue(wire.writingIsComplete());
        wire.rollbackIfNotComplete();

        assertTrue(reader.readOne());
        assertFalse(reader.readOne());
        assertEquals("[pre: pre-C, msg: msg-C]", list.toString());
    }
}
