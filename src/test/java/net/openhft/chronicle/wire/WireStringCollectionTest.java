/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assume.assumeFalse;

public class WireStringCollectionTest extends net.openhft.chronicle.wire.WireTestCommon {
    @Before
    public void hasDirect() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
    }

    @Test
    public void readAndWrite() {
        // Add an alias for ContainsList class
        ClassAliasPool.CLASS_ALIASES.addAlias(ContainsList.class);

        // The string representation of a serialized ContainsList object
        final String hbStr = "!ContainsList {\n" +
                "  list: [\n" +
                "    xx,\n" +
                "    yy\n" +
                "  ],\n" +
                "  map: {\n" +
                "    key: value\n" +
                "  }\n" +
                "}\n";

        // Deserialize hbStr to a ContainsList object
        ContainsList defn = Marshallable.fromString(hbStr);

        // Validate the deserialized list and map contents
        Assert.assertEquals(2, defn.list.size());
        Assert.assertEquals(1, defn.map.size());
        // Validate the string representation of the deserialized object matches the original
        Assert.assertEquals(hbStr, defn.toString());
    }

    // Definition for ContainsList class
    private static class ContainsList extends AbstractEventCfg<ContainsList> {
        @NotNull List<String> list = new ArrayList<>();
        @NotNull Map<String, String> map = new HashMap<>();
    }
}
