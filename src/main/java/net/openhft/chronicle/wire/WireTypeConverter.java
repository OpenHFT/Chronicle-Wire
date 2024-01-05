package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.internal.WireTypeConverterInternal;

/**
 * This is the WireTypeConverter class responsible for converting between different wire types such as JSON and YAML.
 * Internally, it utilizes a delegate pattern with {@link WireTypeConverterInternal} to handle the actual conversion processes.
 */
public class WireTypeConverter {

    // The internal delegate responsible for the actual conversions.
    private final WireTypeConverterInternal delegate;

    /**
     * Constructor initializing the WireTypeConverter with a given Validate instance.
     * The validation mechanism allows for additional checks or constraints during the conversion processes.
     *
     * @param validate The Validate instance to guide conversion operations.
     */
    public WireTypeConverter(Validate validate) {
        delegate = new WireTypeConverterInternal(validate);
    }

    /**
     * Default constructor initializing the WireTypeConverter with a default {@link WireTypeConverterInternal} instance.
     */
    public WireTypeConverter() {
        delegate = new WireTypeConverterInternal();
    }

    /**
     * Converts the given JSON formatted input to its equivalent YAML representation.
     *
     * @param json The JSON input to be converted.
     * @return The converted YAML representation.
     * @throws Exception If any error occurs during conversion.
     */
    public CharSequence jsonToYaml(CharSequence json) throws Exception {
        return delegate.jsonToYaml(json);
    }

    /**
     * Converts the given YAML formatted input to its equivalent JSON representation.
     *
     * @param yaml The YAML input to be converted.
     * @return The converted JSON representation.
     * @throws Exception If any error occurs during conversion.
     */
    public CharSequence yamlToJson(CharSequence yaml) throws Exception {
        return delegate.yamlToJson(yaml);
    }

    /**
     * Associates a given class type with an older type name as an alias.
     * This facilitates backward compatibility or recognition of renamed classes.
     *
     * @param newClass    The new class type.
     * @param oldTypeName The older or previous type name.
     */
    public void addAlias(Class newClass, String oldTypeName) {
        delegate.addAlias(newClass, oldTypeName);
    }
}
