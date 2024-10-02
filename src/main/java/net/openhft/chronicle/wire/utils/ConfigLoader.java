package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.WireType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static net.openhft.chronicle.bytes.util.PropertyReplacer.replaceTokensWithProperties;

/**
 * Utility class for loading configuration files in YAML format. The class provides methods to load configuration
 * files in YAML format from the classpath and convert them to Java objects. The class will replace
 * tokens in the format {@code ${property}} within strings with System properties or supplied properties.
 * <p>
 * Files must be in YAML format that conform to WireType.TEXT. For example:
 * <pre>{@code
 *   !SimpleConfig {
 *      name: "some name",
 *      value: 10,
 *   }
 * }</pre>
 * <p>
 * The class must be fully qualified or added to the {@link net.openhft.chronicle.core.pool.ClassAliasPool} to
 * enable the conversion.
 * <pre>
 * {@code ClassAliasPool.CLASS_ALIASES.addAlias(SimpleConfig.class);}
 * </pre>
 */
public enum ConfigLoader {
    ; // none

    public static String loadFile(Class<?> classLoader, String filename) throws IOException {
        return new String(IOTools.readFile(classLoader, filename), StandardCharsets.UTF_8);
    }

    public static <T> T loadFromFile(String filename) throws IOException {
        return loadFromFile(ConfigLoader.class, filename);
    }

    public static <T> T loadFromFile(Class<?> classLoader, String filename) throws IOException {
        return load(loadFile(classLoader, filename));
    }

    public static <T> T loadFromFile(String filename, Properties properties) throws IOException {
        return loadFromFile(ConfigLoader.class, filename, properties);
    }

    public static <T> T loadFromFile(Class<?> classLoader, String filename, Properties properties) throws IOException {
        return loadWithProperties(loadFile(classLoader, filename), properties);
    }

    public static <T> T loadFromFile(Class<T> expectedClass, Class<?> classLoader, String filename, Properties properties) throws IOException {
        return loadWithProperties(expectedClass, loadFile(classLoader, filename), properties);
    }

    @SuppressWarnings("unchecked")
    public static <T> T load(String fileAsString) {
        return  (T) TextWire.from(replaceTokensWithProperties(fileAsString)).readObject();
    }

    @SuppressWarnings("unchecked")
    public static <T> T loadWithProperties(String fileAsString) {
        return loadWithProperties(fileAsString, System.getProperties());
    }

    @SuppressWarnings("unchecked")
    public static <T> T loadWithProperties(String fileAsString, Properties properties) {
        return loadWithProperties(null, fileAsString, properties);
    }

    @SuppressWarnings("unchecked")
    public static <T> T loadWithProperties(Class<T> expectedClass, String fileAsString) {
        return loadWithProperties(expectedClass, fileAsString, System.getProperties());
    }

    @SuppressWarnings("unchecked")
    public static <T> T loadWithProperties(Class<T> expectedClass, String fileAsString, Properties properties) {
        return WireType.TEXT.fromString(expectedClass, replaceTokensWithProperties(fileAsString, properties));
    }
}
