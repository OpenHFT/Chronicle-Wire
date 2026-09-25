package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.YamlWire;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Issue943Test {
    @Test
    public void test1() {
        YamlWire yaml = new YamlWire(Bytes.valueOf(
                "anchors:\n" +
                "  - !Entry943 {\n" +
                "      object: [\n" +
                "        'foo',\n" +
                "        'bar',\n" +
                "      ],\n" +
                "    },\n" +
                "!Entry943 {\n" +
                "  object: 'baz'\n" +
                "}\n"
        ));

        checks(yaml);
    }

    @Test
    public void test2() {
        YamlWire yaml = new YamlWire(Bytes.valueOf(
                "anchors:\n" +
                "  - !Entry943 {\n" +
                "      object: [\n" +
                "        'foo',\n" +
                "        'bar',\n" +
                "      ]\n" +
                "    }\n" +
                "!Entry943 {\n" +
                "  object: 'baz'\n" +
                "}\n"
        ));

        checks(yaml);
    }

    @Test
    public void test3() {
        YamlWire yaml = new YamlWire(Bytes.valueOf(
                "anchors:\n" +
                "  - !Entry943 {\n" +
                "      object: [\n" +
                "        'foo',\n" +
                "        'bar',\n" +
                "      ],\n" +
                "    },\n---\n" +
                "!Entry943 {\n" +
                "  object: 'baz'\n" +
                "}\n"
        ));

        checks(yaml);
    }

    @Test
    public void test4() {
        YamlWire yaml = new YamlWire(Bytes.valueOf(
                "anchors:\n" +
                "  - !Entry943 {\n" +
                "      object: [\n" +
                "        'foo',\n" +
                "        'bar',\n" +
                "      ]\n" +
                "    }\n---\n" +
                "!Entry943 {\n" +
                "  object: 'baz'\n" +
                "}\n"
        ));

        checks(yaml);
    }

    private static void checks(YamlWire yaml) {

        ClassAliasPool.CLASS_ALIASES.addAlias(Entry943.class);
        List<Object> objects = new ArrayList<>();
        Object object;
        do {
            object = yaml.read().object();

            objects.add(object);
        } while (!yaml.isEmpty() && object != null);

        assertEquals(2, objects.size());
        assertTrue(objects.get(0) instanceof Collection);
        assertTrue(objects.get(1) instanceof Entry943);
        assertTrue(((Collection)objects.get(0)).iterator().next() instanceof Collection);
    }

    public static class Entry943 {
        public Object object;
    }
}
