package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;

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
     * Explicit support for leniency on different types.
     *
     * @param newClass    to use instead
     * @param oldTypeName to support
     */
    public void addAlias(Class<?> newClass, String oldTypeName) {
        jsonWire.classLookup().addAlias(newClass, oldTypeName);
        yamlWire.classLookup().addAlias(newClass, oldTypeName);
    }
}
