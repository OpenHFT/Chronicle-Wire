package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.wire.internal.WireTypeConverterInternal;
import org.jetbrains.annotations.NotNull;

public class WireTypeConverter {
    private final WireTypeConverterInternal delegate;

    public WireTypeConverter(@NotNull Validate validate, @NotNull ClassLookup classLookup) {
        delegate = new WireTypeConverterInternal(validate, classLookup);
    }

    public WireTypeConverter(@NotNull ClassLookup classLookup) {
        delegate = new WireTypeConverterInternal(classLookup);
    }

    public WireTypeConverter(@NotNull Validate validate) {
        delegate = new WireTypeConverterInternal(validate);
    }

    public WireTypeConverter() {
        delegate = new WireTypeConverterInternal();
    }

    public CharSequence jsonToYaml(CharSequence json) {
        return delegate.jsonToYaml(json);
    }

    public CharSequence yamlToJson(CharSequence yaml) {
        return delegate.yamlToJson(yaml);
    }

    public void addAlias(Class<?> newClass, String oldTypeName) {
        delegate.addAlias(newClass, oldTypeName);
    }
}
