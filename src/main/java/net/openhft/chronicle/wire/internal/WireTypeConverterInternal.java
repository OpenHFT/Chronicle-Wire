package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;

/**
 * <b>INTERNAL USE ONLY.</b> Implementation delegate for {@link net.openhft.chronicle.wire.WireTypeConverter}.
 * It manages {@link net.openhft.chronicle.wire.JSONWire} and {@link net.openhft.chronicle.wire.YamlWire}
 * instances to perform conversions by parsing the input with one wire type and then using
 * {@link net.openhft.chronicle.wire.WireIn#copyTo(net.openhft.chronicle.wire.WireOut)} to serialise to the other.
 */
public class WireTypeConverterInternal {
    /** Internal {@link Wire} instance configured for YAML processing using an elastic on-heap buffer. */
    private final Wire yamlWire = WireType.YAML_ONLY.apply(Bytes.allocateElasticOnHeap());
    /** Internal {@link Wire} instance configured for JSON processing using an elastic on-heap buffer. */
    private final Wire jsonWire = WireType.JSON_ONLY.apply(Bytes.allocateElasticOnHeap());

    /**
     * Converts the JSON {@link CharSequence} to YAML.
     * The input {@code json} is appended to {@link #jsonWire} and copied to {@link #yamlWire}.
     *
     * @param json the JSON data
     * @return the contents of {@link #yamlWire}
     */
    public CharSequence jsonToYaml(CharSequence json) {
        jsonWire.reset();
        jsonWire.bytes().append(json);

        jsonWire.copyTo(yamlWire);

        return yamlWire.bytes();
    }

    /**
     * Converts the YAML {@link CharSequence} to JSON.
     * The input {@code yaml} is appended to {@link #yamlWire} and copied to {@link #jsonWire}.
     *
     * @param yaml the YAML data
     * @return the contents of {@link #jsonWire}
     */
    public CharSequence yamlToJson(CharSequence yaml) {
        yamlWire.reset();
        yamlWire.bytes().clear().append(yaml);

        jsonWire.bytes().clear();
        yamlWire.copyTo(jsonWire);

        return jsonWire.bytes();
    }

    /**
     * Adds the class alias to the {@link net.openhft.chronicle.core.pool.ClassLookup}
     * of both {@link #jsonWire} and {@link #yamlWire}.
     *
     * @param newClass   the class the alias maps to
     * @param oldTypeName the legacy type name
     */
    public void addAlias(Class<?> newClass, String oldTypeName) {
        jsonWire.classLookup().addAlias(newClass, oldTypeName);
        yamlWire.classLookup().addAlias(newClass, oldTypeName);
    }
}
