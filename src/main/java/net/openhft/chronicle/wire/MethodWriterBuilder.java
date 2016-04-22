package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.util.ObjectUtils;

import java.lang.reflect.Proxy;
import java.util.function.Supplier;

/**
 * Created by peter on 28/03/16.
 */
public class MethodWriterBuilder<T> implements Supplier<T> {
    public static final Class[] NO_CLASSES = {};
    private final MarshallableOut out;
    private final Class<T> tClass;
    private final MethodWriterInvocationHandler handler;
    private Class[] additionalClasses = NO_CLASSES;

    public MethodWriterBuilder(MarshallableOut out, Class<T> tClass) {
        this.out = out;
        this.tClass = tClass;
        handler = new MethodWriterInvocationHandler(out);
    }

    public MethodWriterBuilder<T> additionalInterfaces(Class... additionalClasses) {
        this.additionalClasses = additionalClasses;
        return this;
    }

    public MethodWriterBuilder<T> recordHistory(boolean recordHistory) {
        handler.recordHistory(recordHistory);
        return this;
    }

    @Override
    public T get() {
        Class[] interfaces = ObjectUtils.addAll(tClass, additionalClasses);
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(tClass.getClassLoader(), interfaces, handler);

    }
}
