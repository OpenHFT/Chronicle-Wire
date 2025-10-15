package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.After;
import org.junit.Test;

import java.util.Properties;

public class ConfigLoaderInjectionTest extends WireTestCommon {
    private static final String YAML = "name: ${name}";

    @After
    public void clearProperty() {
        System.clearProperty("name");
    }

    @Test(expected = IllegalArgumentException.class)
    public void loadWithPropertiesRejectsNewLine() {
        Properties p = new Properties();
        p.setProperty("name", "bad\nvalue");
        ConfigLoader.loadWithProperties(YAML, p);
    }

    @Test(expected = IllegalArgumentException.class)
    public void loadRejectsNewLineInSystemProperty() {
        System.setProperty("name", "bad\nvalue");
        ConfigLoader.load(YAML);
    }
}
