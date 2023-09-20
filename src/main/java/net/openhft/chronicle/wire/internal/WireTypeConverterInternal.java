package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;

/**
 * Provides the capability to convert between different wire types, with a primary focus on
 * converting from JSON to YAML format.
 *
 * This class encapsulates both JSON and YAML wire types to facilitate the conversion.
 * In addition to conversion, it supports validation mechanisms to ensure correctness and integrity of
 * the transformed data.
 *
 * @since 2023-09-16
 */
public class WireTypeConverterInternal {
    private final Wire yamlWire = WireType.YAML_ONLY.apply(Bytes.allocateElasticOnHeap());
    private final Wire jsonWire = WireType.JSON_ONLY.apply(Bytes.allocateElasticOnHeap());

    public CharSequence jsonToYaml(CharSequence json) {
        jsonWire.reset();
        jsonWire.bytes().append(json);

        jsonWire.copyTo(yamlWire);

        return yamlWire.bytes();
    }

    public CharSequence yamlToJson(CharSequence yaml) {
        yamlWire.reset();
        yamlWire.bytes().clear().append(yaml);

        jsonWire.bytes().clear();
        yamlWire.copyTo(jsonWire);

        return jsonWire.bytes();
    }


    /**
     * Adds aliasing support for type leniency. This facilitates the serialization and deserialization
     * of objects whose class names might have changed. By providing an alias, the system can recognize
     * and handle the renamed class seamlessly.
     *
     * @param newClass    The new class type to use for serialization and deserialization.
     * @param oldTypeName The old type name that this new class is an alias for.
     */
    public void addAlias(Class newClass, String oldTypeName) {
        jsonWire.classLookup().addAlias(newClass, oldTypeName);
        yamlWire.classLookup().addAlias(newClass, oldTypeName);
    }
}
