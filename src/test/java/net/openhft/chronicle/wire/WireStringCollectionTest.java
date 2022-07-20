package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WireStringCollectionTest {

    @Test
    public void readAndWrite() {
        ClassAliasPool.CLASS_ALIASES.addAlias(ContainsList.class);
        final String hbStr = "!ContainsList {\n" +
                "  list: [\n" +
                "    xx,\n" +
                "    yy\n" +
                "  ],\n" +
                "  map: {\n" +
                "    key: value\n" +
                "  }\n" +
                "}\n";
        ContainsList defn = Marshallable.fromString(hbStr);
        Assert.assertEquals(2, defn.list.size());
        Assert.assertEquals(1, defn.map.size());
        Assert.assertEquals(hbStr, defn.toString());
    }

    private static class ContainsList extends AbstractEventCfg<ContainsList> {
        @NotNull List<String> list = new ArrayList<>();
        @NotNull Map<String, String> map = new HashMap<>();
    }
}