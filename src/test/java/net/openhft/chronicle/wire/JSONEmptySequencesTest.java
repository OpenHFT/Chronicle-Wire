package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Assert;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

public class JSONEmptySequencesTest {
    @Test
    public void emptySequence() {
        ClassAliasPool.CLASS_ALIASES.addAlias(Foo.class);

        final Bytes data = Bytes.elasticByteBuffer();
        data.append("!Foo {\n" +
                "  field1: 1234,\n" +
                "  field2: 456,\n" +
                "  field3: [ ],\n" +
                "  field4: [\n" +
                "    abc,\n" +
                "    xyz\n" +
                "  ]\n" +
                "}");

        final JSONWire wire = new JSONWire(data);
        final Foo f = new Foo();
        wire.getValueIn().object(f, Foo.class);

        Assert.assertEquals("!Foo {\n" +
                "  field1: 1234,\n" +
                "  field2: 456,\n" +
                "  field3: [ ],\n" +
                "  field4: [\n" +
                "    abc,\n" +
                "    xyz\n" +
                "  ]\n" +
                "}\n", f.toString());
    }

    private static final class Foo extends SelfDescribingMarshallable {
        int field1;
        int field2;
        final List<String> field3 = new ArrayList<>();
        final List<String> field4 = new ArrayList<>();
    }
}