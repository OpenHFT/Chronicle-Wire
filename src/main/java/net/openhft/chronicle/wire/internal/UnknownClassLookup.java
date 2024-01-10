package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import net.openhft.compiler.CachedCompiler;

import java.io.File;

public class UnknownClassLookup implements ClassLookup {
    public static final CachedCompiler CACHED_COMPILER =
        new CachedCompiler(Jvm.isDebug() ? new File(OS.getTarget(), "generated-test-sources") : null, null);

    private final ClassLookup delegate;

    public UnknownClassLookup(ClassLookup delegate) {
        this.delegate = delegate;
    }

    @Override
    public Class<?> forName(CharSequence name) throws ClassNotFoundRuntimeException {
        try {
            return delegate.forName(name);
        } catch (Exception e) {
            String className = name.toString();
            Class<?> unknownClass;
            try {
                unknownClass = CACHED_COMPILER.loadFromJava(className,
                    "public class " + className + " extends " + UnknownClassBase.class.getName() + "{}");
            } catch (ClassNotFoundException ex) {
                throw new ClassNotFoundRuntimeException(ex);
            }

            addAlias(unknownClass, className);
            return delegate.forName(name);
        }
    }

    @Override
    public String nameFor(Class<?> clazz) throws IllegalArgumentException {
        return delegate.nameFor(clazz);
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
