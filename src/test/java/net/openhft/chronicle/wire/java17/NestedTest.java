package net.openhft.chronicle.wire.java17;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NestedTest extends WireTestCommon {

    @Test
    public void mini() {
        ClassAliasPool.CLASS_ALIASES.addAlias(Group.class);
        Field field = new Field();
        Group g = new Group(field);
        field.required("parent", Required.NO);
        assertEquals("!Group {\n" +
                "  field: {\n" +
                "    required: {\n" +
                "      parent: NO\n" +
                "    }\n" +
                "  }\n" +
                "}\n", g.toString());
    }
}
