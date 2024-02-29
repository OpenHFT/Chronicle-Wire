package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.internal.WireTypeConverterInternal;

public class WireTypeConverter {
    private final WireTypeConverterInternal delegate;


    public WireTypeConverter() {
        delegate = new WireTypeConverterInternal();
    }

    public CharSequence jsonToYaml(CharSequence json) {
        return delegate.jsonToYaml(json);
    }

    public CharSequence yamlToJson(CharSequence yaml) {
        return delegate.yamlToJson(yaml);
    }

    public void addAlias(Class newClass, String oldTypeName) {
        delegate.addAlias(newClass, oldTypeName);
    }
}
