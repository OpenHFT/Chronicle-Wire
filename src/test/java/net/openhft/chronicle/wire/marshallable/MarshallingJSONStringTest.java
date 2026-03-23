/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MarshallingJSONStringTest implements Marshallable {

    private String configAsJSON;

    @Override
    public void writeMarshallable(@NotNull final WireOut wire) throws InvalidMarshallableException {
        wire.write("config").text(configAsJSON);
    }

    @Override
    public void readMarshallable(@NotNull final WireIn wire) throws IORuntimeException, InvalidMarshallableException {
        configAsJSON = wire.read("config").text();
    }

    @Test
    public void testNoPrefixAddedToJson() {

        String configJson = "!net.openhft.chronicle.wire.marshallable.MarshallingJSONStringTest {\n" +
                "  config: {\n" +
                "    \"username\": \"sampleApp\",\n" +
                "    \"password\": \"samplePassword\",\n" +
                "    \"publishPort\": 4021,\n" +
                "    \"subscribePort\": 4024,\n" +
                "  }\n" +
                "}";
        String expectedJson = "{\n" +
        "    \"username\": \"sampleApp\",\n" +
                "    \"password\": \"samplePassword\",\n" +
                "    \"publishPort\": 4021,\n" +
                "    \"subscribePort\": 4024,\n" +
                "  }";

        MarshallingJSONStringTest read = Marshallable.fromString(configJson);
        assertEquals(expectedJson, read.configAsJSON);
    }

    @Test
    public void readingJsonListWithNestedSingleListAsLastElement() {
        ClassAliasPool.CLASS_ALIASES.addAlias(NestedList.class);
        String jsonText = "{\n" +
                "  items: [\n" +
                "    { name: item1, value: 100, subItems: { subName: subItem1, subValue: 10 } },\n" +
                "    { name: item2, value: 200, subItems: [ { subName: subItem2, subValue: 20 },         { subName: subItem3, subValue: 30 } ] }\n" +
                "  ]\n" +
                "}";

        String expectedJson = "!NestedList {\n" +
                "  items: [\n" +
                "    { name: item1, value: 100, subItems: [ { subName: subItem1, subValue: 10 } ], otherValue: !!null \"\" },\n" +
                "    { name: item2, value: 200, subItems: [ { subName: subItem2, subValue: 20 },         { subName: subItem3, subValue: 30 } ], otherValue: !!null \"\" }\n" +
                "  ]\n" +
                "}\n";

        NestedList read = Marshallable.fromString(NestedList.class, jsonText);
        assertNotNull(read);
        String actualJson = read.toString();
        assertEquals(expectedJson, actualJson);
    }

    @Test
    public void readingJsonListWithNestedSingleListInMiddle() {
        ClassAliasPool.CLASS_ALIASES.addAlias(NestedList.class);
        String jsonText = "{\n" +
                "  items: [\n" +
                "    { name: item1, value: 100, subItems: { subName: subItem1, subValue: 10 }, otherValue: value1 },\n" +
                "    { name: item2, value: 200, subItems: [ { subName: subItem2, subValue: 20 },         { subName: subItem3, subValue: 30 } ], otherValue: value2 }\n" +
                "  ]\n" +
                "}";

        String expectedJson = "!NestedList {\n" +
                "  items: [\n" +
                "    { name: item1, value: 100, subItems: [ { subName: subItem1, subValue: 10 } ], otherValue: value1 },\n" +
                "    { name: item2, value: 200, subItems: [ { subName: subItem2, subValue: 20 },         { subName: subItem3, subValue: 30 } ], otherValue: value2 }\n" +
                "  ]\n" +
                "}\n";

        NestedList read = Marshallable.fromString(NestedList.class, jsonText);
        assertNotNull(read);
        String actualJson = read.toString();
        assertEquals(expectedJson, actualJson);
    }
}
