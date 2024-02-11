package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import net.openhft.chronicle.wire.Validate;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class WireTypeConverterInternal {
    private static final Validate NO_OP = x -> {
    };
    private final Bytes<?> bytes = Bytes.allocateElasticOnHeap();
    private final Wire yamlWire = WireType.YAML_ONLY.apply(bytes);
    private final Wire jsonWire = WireType.JSON_ONLY.apply(bytes);

    private final Validate validate;

    private final ExceptionCatchingClassLookup exceptionCatchingClassLookup;

    private Exception e;

    public WireTypeConverterInternal(@NotNull Validate validate, @NotNull ClassLookup classLookup) {
        this.validate = validate;
        this.exceptionCatchingClassLookup =
            new ExceptionCatchingClassLookup(classLookup, this::onException);
        jsonWire.classLookup(exceptionCatchingClassLookup);
        yamlWire.classLookup(exceptionCatchingClassLookup);
    }

    public WireTypeConverterInternal(@NotNull Validate validate) {
        this(validate, ClassAliasPool.CLASS_ALIASES);
    }

    public WireTypeConverterInternal(@NotNull ClassLookup classLookup) {
        this(NO_OP, classLookup);
    }

    public WireTypeConverterInternal() {
        this(NO_OP, ClassAliasPool.CLASS_ALIASES);
    }

    private void onException(Exception e) {
        this.e = e;
    }

    public CharSequence jsonToYaml(CharSequence json) {
        e = null;
        jsonWire.reset();
        jsonWire.bytes().append(json);

        Object object = jsonWire.getValueIn().object();
        if (e != null)
            throw Jvm.rethrow(e);

        validate.validate(object);
        yamlWire.reset();
        yamlWire.getValueOut().object(object);
        if (e != null)
            throw Jvm.rethrow(e);

        return yamlWire.bytes();
    }

    public CharSequence yamlToJson(CharSequence yaml) {
        e = null;
        yamlWire.reset();
        yamlWire.bytes().clear().append(yaml);
        Object object = yamlWire.getValueIn().object();

        if (e != null)
            throw Jvm.rethrow(e);

        validate.validate(object);
        jsonWire.reset();
        jsonWire.bytes().clear();
        jsonWire.getValueOut().object(object);
        if (e != null)
            throw Jvm.rethrow(e);

        return jsonWire.bytes();
    }

    private static class ExceptionCatchingClassLookup implements ClassLookup {
        private final ClassLookup delegate;
        private final Consumer<Exception> onException;

        private ExceptionCatchingClassLookup(ClassLookup delegate, Consumer<Exception> onException) {
            this.delegate = delegate;
            this.onException = onException;
        }

        @Override
        public Class<?> forName(CharSequence name) throws ClassNotFoundRuntimeException {
            try {
                return delegate.forName(name);
            } catch (Exception e) {
                onException.accept(e);
                throw e;
            }
        }

        @Override
        public String nameFor(Class<?> clazz) throws IllegalArgumentException {
            try {
                return delegate.nameFor(clazz);
            } catch (Exception e) {
                onException.accept(e);
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
    }

    /**
     * Explicit support for leniency on different types.
     *
     * @param newClass    to use instead
     * @param oldTypeName to support
     */
    public void addAlias(Class<?> newClass, String oldTypeName) {
        exceptionCatchingClassLookup.addAlias(newClass, oldTypeName);
    }
}
