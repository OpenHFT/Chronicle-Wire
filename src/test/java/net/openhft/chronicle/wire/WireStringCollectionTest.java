/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WireStringCollectionTest extends net.openhft.chronicle.wire.WireTestCommon {

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
