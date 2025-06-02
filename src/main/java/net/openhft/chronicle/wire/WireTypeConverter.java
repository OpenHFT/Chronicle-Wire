package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.internal.WireTypeConverterInternal;

/**
 * Converts data between wire types such as JSON and YAML.
 * The work is delegated to {@link WireTypeConverterInternal}.
 */
public class WireTypeConverter {

    // The internal delegate responsible for the actual conversions.
    private final WireTypeConverterInternal delegate;

    /**
     * Default constructor.
     * Initialises the internal delegate {@link net.openhft.chronicle.wire.internal.WireTypeConverterInternal}.
     */
    public WireTypeConverter() {
        delegate = new WireTypeConverterInternal();
    }

    /**
     * Converts the given JSON formatted input to its YAML representation.
     *
     * @param json the JSON formatted {@link CharSequence} to be converted
     * @return a {@link CharSequence} containing the YAML representation of the input JSON
     */
    public CharSequence jsonToYaml(CharSequence json) {
        return delegate.jsonToYaml(json);
    }

    /**
     * Converts the given YAML formatted input to its JSON representation.
     *
     * @param yaml the YAML formatted {@link CharSequence} to be converted
     * @return a {@link CharSequence} containing the JSON representation of the input YAML
     */
    public CharSequence yamlToJson(CharSequence yaml) {
        return delegate.yamlToJson(yaml);
    }

    /**
     * Registers a class alias that will be used during conversions.
     * This allows a type name {@code oldTypeName} seen in the input to be mapped to
     * {@code newClass} during deserialisation by the internal wires.
     *
     * @param newClass   the target {@link Class} object
     * @param oldTypeName the alternative or old type name string that should map to {@code newClass}
     */
    public void addAlias(Class<?> newClass, String oldTypeName) {
        delegate.addAlias(newClass, oldTypeName);
    }
}
