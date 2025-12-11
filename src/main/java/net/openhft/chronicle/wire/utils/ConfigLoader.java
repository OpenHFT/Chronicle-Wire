/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.wire.TextWire;

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

    /**
     * Loads the contents of the given file using the supplied class' classloader.
     *
     * @param classLoader class whose loader should resolve the resource
     * @param filename    path to the configuration file
     * @return file contents as a UTF-8 string
     * @throws IOException if the resource cannot be read
     */
    public static String loadFile(Class<?> classLoader, String filename) throws IOException {
        return new String(IOTools.readFile(classLoader, filename), StandardCharsets.UTF_8);
    }

    /**
     * Loads and parses a YAML file using this class' classloader.
     *
     * @param filename resource name to read
     * @param <T>      target type
     * @return deserialized configuration instance
     * @throws IOException if the file cannot be read
     */
    public static <T> T loadFromFile(String filename) throws IOException {
        return loadFromFile(ConfigLoader.class, filename);
    }

    /**
     * Loads and parses a YAML file using the supplied classloader.
     *
     * @param classLoader class whose loader should resolve the resource
     * @param filename    resource name to read
     * @param <T>         target type
     * @return deserialized configuration instance
     * @throws IOException if the file cannot be read
     */
    public static <T> T loadFromFile(Class<?> classLoader, String filename) throws IOException {
        return load(loadFile(classLoader, filename));
    }

    /**
     * Loads and parses a YAML file after property substitution using the default classloader.
     *
     * @param filename   resource name to read
     * @param properties properties to substitute into the file content
     * @param <T>        target type
     * @return deserialized configuration instance
     * @throws IOException if the file cannot be read
     */
    public static <T> T loadFromFile(String filename, Properties properties) throws IOException {
        return loadFromFile(ConfigLoader.class, filename, properties);
    }

    /**
     * Loads and parses a YAML file after property substitution using the supplied classloader.
     *
     * @param classLoader class whose loader should resolve the resource
     * @param filename    resource name to read
     * @param properties  properties to substitute into the file content
     * @param <T>         target type
     * @return deserialized configuration instance
     * @throws IOException if the file cannot be read
     */
    public static <T> T loadFromFile(Class<?> classLoader, String filename, Properties properties) throws IOException {
        return loadWithProperties(loadFile(classLoader, filename), properties);
    }

    /**
     * Parses configuration text, replacing property tokens using system properties.
     *
     * @param fileAsString YAML configuration text
     * @param <T>          target type
     * @return deserialized configuration instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T load(String fileAsString) {
        return  (T) TextWire.from(replaceTokensWithProperties(fileAsString)).readObject();
    }

    /**
     * Parses configuration text after property substitution using supplied properties.
     *
     * @param fileAsString YAML configuration text
     * @param properties   substitution properties
     * @param <T>          target type
     * @return deserialized configuration instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T loadWithProperties(String fileAsString, Properties properties) {
        return (T) TextWire.from(replaceTokensWithProperties(fileAsString, properties)).readObject();
    }
}
