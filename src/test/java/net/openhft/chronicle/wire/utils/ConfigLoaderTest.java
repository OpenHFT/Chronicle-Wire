package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.AbstractMarshallableCfg;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigLoaderTest {
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(SimpleConfig.class);
    }

    @Test
    public void testLoadFromFile() throws IOException {

        SimpleConfig config = ConfigLoader.loadFromFile("utils/simple-config.yaml");
        assertEquals("some name", config.name());
        assertEquals(10, config.value());
    }

    @Test
    public void testLoadFromFileWithProperties() throws IOException {
        Properties properties = new Properties();
        properties.put("config-name", "some name");
        SimpleConfig config = ConfigLoader.loadFromFile("utils/simple-config-properties.yaml", properties);
        assertEquals("some name", config.name());
        assertEquals(10, config.value());
    }

    public static class SimpleConfig extends AbstractMarshallableCfg {
        private String name;
        private int value;

        @SuppressWarnings("unused")
        public SimpleConfig() {
        }

        public String name() {
            return name;
        }

        public int value() {
            return value;
        }

        public void name(String name) {
            this.name = name;
        }

        public void value(int value) {
            this.value = value;
        }
    }

}
