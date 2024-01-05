package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import net.openhft.chronicle.wire.Validate;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;

/**
 * Provides the capability to convert between different wire types, with a primary focus on
 * converting from JSON to YAML format.
 *
 * This class encapsulates both JSON and YAML wire types to facilitate the conversion.
 * In addition to conversion, it supports validation mechanisms to ensure correctness and integrity of
 * the transformed data.
 */
public class WireTypeConverterInternal {

    // No-operation validator which performs no action during validation
    private static final Validate NO_OP = x -> {
    };

    // Bytes instance for dynamically managing the serialized representation
    private final Bytes bytes = Bytes.allocateElasticOnHeap();

    // Dedicated wire instances for YAML and JSON serialization/deserialization
    private final Wire yamlWire = WireType.YAML_ONLY.apply(bytes);
    private final Wire jsonWire = WireType.JSON_ONLY.apply(bytes);

    // Validator to ensure the correctness and integrity of the data during transformation
    private final Validate validate;

    // Exception instance to handle any exceptions that arise during conversion
    private Exception e;

    /**
     * Constructs a WireTypeConverterInternal object with the provided validator.
     *
     * @param validate The validator to ensure the correctness and integrity of the data during transformation.
     */
    public WireTypeConverterInternal(@NotNull Validate validate) {
        this.validate = validate;
        replaceClassLookup(jsonWire);
        replaceClassLookup(yamlWire);
    }

    /**
     * Constructs a WireTypeConverterInternal object with a default no-operation validator.
     */
    public WireTypeConverterInternal() {
        this.validate = NO_OP;
        replaceClassLookup(jsonWire);
        replaceClassLookup(yamlWire);
    }

    /**
     * Converts the provided JSON formatted CharSequence into its equivalent YAML format.
     * The conversion is achieved through deserialization into an intermediate object representation
     * and subsequent serialization to the target format.
     *
     * @param json The input JSON formatted CharSequence.
     * @return A CharSequence containing the data in YAML format.
     * @throws Exception If any exception occurs during the conversion or validation process.
     */
    public CharSequence jsonToYaml(CharSequence json) throws Exception {
        e = null;
        jsonWire.reset();
        jsonWire.bytes().append(json);

        Object object = jsonWire.getValueIn().object();
        if (e != null)
            throw e;
        validate.validate(object);
        yamlWire.reset();
        yamlWire.getValueOut().object(object);
        if (e != null)
            throw e;

        return yamlWire.bytes();
    }

    /**
     * Converts the provided YAML formatted CharSequence into its equivalent JSON format.
     * The conversion involves deserialization of the YAML into an intermediate object representation
     * followed by serialization into the JSON format.
     *
     * @param yaml The input YAML formatted CharSequence.
     * @return A CharSequence containing the data in JSON format.
     * @throws Exception If any exception occurs during the conversion or validation process.
     */
    public CharSequence yamlToJson(CharSequence yaml) throws Exception {
        e = null;
        yamlWire.reset();
        yamlWire.bytes().clear().append(yaml);
        Object object = yamlWire.getValueIn().object();

        if (e != null)
            throw e;

        validate.validate(object);
        jsonWire.reset();
        jsonWire.bytes().clear();
        jsonWire.getValueOut().object(object);
        if (e != null)
            throw e;
        return jsonWire.bytes();
    }

    /**
     * Replaces the class lookup logic of the provided wire with a custom implementation that
     * captures and holds exceptions. This allows for custom error handling when resolving classes
     * during serialization or deserialization.
     *
     * @param wire The wire whose class lookup will be replaced.
     */
    private void replaceClassLookup(Wire wire) {
        final ClassLookup delegate = wire.classLookup();
        wire.classLookup(new ClassLookup() {

            @Override
            public Class<?> forName(CharSequence name) throws ClassNotFoundRuntimeException {
                try {
                    return delegate.forName(name);
                } catch (Exception e) {
                    WireTypeConverterInternal.this.e = e;
                    throw e;
                }
            }

            @Override
            public String nameFor(Class<?> clazz) throws IllegalArgumentException {
                try {
                    return delegate.nameFor(clazz);
                } catch (Exception e) {
                    WireTypeConverterInternal.this.e = e;
                    throw e;
                }
            }

            @Override
            public void addAlias(Class<?>... classes) {
                delegate.addAlias(classes);
            }

            @Override
            public void addAlias(Class<?> clazz, String names) {
                delegate.addAlias(clazz, names);
            }
        });
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
