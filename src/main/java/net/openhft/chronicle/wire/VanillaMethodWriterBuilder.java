/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.util.Builder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@SuppressWarnings({"rawtypes", "unchecked"})
public class VanillaMethodWriterBuilder<T> implements Builder<T>, MethodWriterBuilder<T> {
    public static final String DISABLE_WRITER_PROXY_CODEGEN = "disableProxyCodegen";

    private static final Class<?> COMPILE_FAILED = ClassNotFoundException.class;
    private static final Map<String, Class> classCache = new ConcurrentHashMap<>();
    private static final List<Class> invalidSuperInterfaces = Arrays.asList(
            ReadBytesMarshallable.class,
            WriteBytesMarshallable.class,
            ReadMarshallable.class,
            WriteMarshallable.class,
            Collection.class,
            Map.class,
            Iterator.class,
            Iterable.class,
            Comparable.class,
            Serializable.class,
            CharSequence.class,
            Comparable.class,
            Comparator.class,
            Observer.class
    );

    private final boolean disableProxyGen = Jvm.getBoolean(DISABLE_WRITER_PROXY_CODEGEN, false);
    private final Set<Class<?>> interfaces = Collections.synchronizedSet(new LinkedHashSet<>());

    private final String packageName;
    private ClassLoader classLoader;
    @NotNull
    private final MethodWriterInvocationHandlerSupplier handlerSupplier;
    private Supplier<MarshallableOut> outSupplier;
    private Closeable closeable;
    private String genericEvent;
    private boolean metaData;
    private WireType wireType;
    private Class<?> proxyClass;
    private UpdateInterceptor updateInterceptor;

    public VanillaMethodWriterBuilder(@NotNull Class<T> tClass,
                                      WireType wireType,
                                      @NotNull Supplier<MethodWriterInvocationHandler> handlerSupplier) {
        this.packageName = tClass.getPackage().getName();
        this.wireType = wireType;
        addInterface(tClass);
        this.classLoader = tClass.getClassLoader();
        this.handlerSupplier = new MethodWriterInvocationHandlerSupplier(handlerSupplier);
    }

    @NotNull
    public MethodWriterBuilder<T> classLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    /**
     * @param updateInterceptor used to modify the data before it is written to the wire
     */
    @Override
    @NotNull
    public MethodWriterBuilder<T> updateInterceptor(UpdateInterceptor updateInterceptor) {
        this.updateInterceptor = updateInterceptor;
        return this;
    }

    @NotNull
    public MethodWriterBuilder<T> addInterface(Class additionalClass) {
        if (interfaces.contains(additionalClass))
            return this;
        if (additionalClass == DocumentContext.class)
            return this;

        for (Class invalidSuperInterface : invalidSuperInterfaces) {
            if (invalidSuperInterface.isAssignableFrom(additionalClass))
                throw new IllegalArgumentException("The event interface shouldn't implement " + invalidSuperInterface.getName());
        }
        interfaces.add(additionalClass);
        for (Method method : additionalClass.getMethods()) {
            Class<?> returnType = method.getReturnType();
            if (returnType.isInterface() && !Jvm.dontChain(returnType)) {
                addInterface(returnType);
            }
        }
        return this;
    }

    @Deprecated(/* Replaced by UpdateInterceptor. To be removed in x.24 */)
    @NotNull
    public MethodWriterBuilder<T> methodWriterInterceptorReturns(MethodWriterInterceptorReturns methodWriterInterceptor) {
        Jvm.warn().on(getClass(), "Support for methodWriterInterceptorReturns will be dropped in x.24. Use UpdateInterceptor instead");
        handlerSupplier.methodWriterInterceptorReturns(methodWriterInterceptor);
        return this;
    }

    @NotNull
    public MethodWriterBuilder<T> disableThreadSafe(boolean theadSafe) {
        handlerSupplier.disableThreadSafe(theadSafe);
        return this;
    }

    @NotNull
    public T build() {
        return get();
    }

    @NotNull
    public MethodWriterBuilder<T> onClose(Closeable closeable) {
        this.closeable = closeable;
        handlerSupplier.onClose(closeable);
        return this;
    }

    public WireType wireType() {
        return wireType;
    }

    public VanillaMethodWriterBuilder<T> wireType(final WireType wireType) {
        this.wireType = wireType;
        return this;
    }

    /**
     * because we cache the classes in {@code classCache}, its very important to come up with a name that is unique for what the class does.
     *
     * @return the name of the new class
     */
    @NotNull
    private String getClassName() {
        final StringBuilder sb = new StringBuilder();

        interfaces.forEach(i -> {
            if (i.getEnclosingClass() != null)
                sb.append(i.getEnclosingClass().getSimpleName());
            sb.append(i.getSimpleName());
        });
        sb.append(this.genericEvent == null ? "" : this.genericEvent);
        sb.append(this.metaData ? "MetadataAware" : "");
        sb.append(updateInterceptor != null ? "Intercepting" : "");
        sb.append(toFirstCapCase(wireType().toString().replace("_", "")));
        sb.append("MethodWriter");
        return sb.toString();
    }

    @NotNull
    @Override
    public T get() {
        if (proxyClass != null) {
            try {
                Constructor<T> constructor = (Constructor) proxyClass.getConstructor(MethodWriterInvocationHandlerSupplier.class);
                return constructor.newInstance(handlerSupplier);
            } catch (Throwable e) {
                // do nothing and drop through
                if (Jvm.isDebugEnabled(getClass()))
                    Jvm.debug().on(getClass(), e);
            }
        }
        if (!disableProxyGen && handlerSupplier.methodWriterInterceptorReturns() == null) {
            T t = createInstance();
            if (t != null)
                return t;
        } else {
            Jvm.warn().on(getClass(), "Falling back to proxy method writer. Support for " +
                    "proxy method writers will be dropped in x.25.");
        }

        @NotNull Class[] interfacesArr = interfaces.toArray(new Class[interfaces.size()]);

        //noinspection unchecked
        return (T) Proxy.newProxyInstance(classLoader, interfacesArr, new CallSupplierInvocationHandler(this));
    }

    @Nullable
    private T createInstance() {
        String fullClassName = packageName + "." + getClassName();
        try {
            try {
                return (T) newInstance(Class.forName(fullClassName));
            } catch (ClassNotFoundException e) {
                Class clazz = classCache.computeIfAbsent(fullClassName, this::newClass);
                if (clazz != null && clazz != COMPILE_FAILED) {
                    return (T) newInstance(clazz);
                }
            }
        } catch (MethodWriterValidationException e) {
            throw e;
        } catch (Throwable e) {
            classCache.put(fullClassName, COMPILE_FAILED);
            Jvm.warn().on(getClass(), "Failed to compile generated method writer - " +
                    "falling back to proxy method writer. Please report this failure as support for " +
                    "proxy method writers will be dropped in x.25.", e);
        }
        return null;
    }

    private Class newClass(final String fullClassName) {
        if (wireType.isText() || !Jvm.getBoolean("wire.generator.v2"))
            return GenerateMethodWriter.newClass(fullClassName,
                    interfaces,
                    classLoader,
                    wireType,
                    genericEvent,
                    metaData,
                    true,
                    updateInterceptor != null);
        GenerateMethodWriter2 gmw = new GenerateMethodWriter2();
        gmw.metaData()
                .packageName(fullClassName.substring(0, fullClassName.lastIndexOf('.')))
                .baseClassName(fullClassName.substring(1 + fullClassName.lastIndexOf('.')))
                .interfaces(interfaces)
                .genericEvent(genericEvent)
                .metaData(metaData)
                .useMethodIds(true)
                .useUpdateInterceptor(updateInterceptor != null);
        gmw.maxCode(0);
        return gmw.acquireClass(classLoader);
    }

    private Object newInstance(final Class aClass) {
        try {
            if (outSupplier == null)
                throw new NullPointerException("marshallableOut(out) has not been set.");
            if (outSupplier.get().recordHistory()) {
                handlerSupplier.recordHistory(true);
            }
            return aClass.getDeclaredConstructors()[0].newInstance(outSupplier, closeable, updateInterceptor);
        } catch (Exception e) {
            throw Jvm.rethrow(e);
        }
    }

    /**
     * A generic event treats the first argument as the eventName
     *
     * @param genericEvent name
     * @return this
     */
    public MethodWriterBuilder<T> genericEvent(String genericEvent) {
        handlerSupplier.genericEvent(genericEvent);
        this.genericEvent = genericEvent;
        return this;
    }

    public MethodWriterBuilder<T> marshallableOut(@NotNull final MarshallableOut out) {
        this.outSupplier = () -> out;
        return this;
    }

    public MethodWriterBuilder<T> marshallableOutSupplier(@NotNull final Supplier<MarshallableOut> out) {
        this.outSupplier = out;
        return this;
    }

    @Override
    public MethodWriterBuilder<T> metaData(final boolean metaData) {
        this.metaData = metaData;
        return this;
    }

    public Class<?> proxyClass() {
        return proxyClass;
    }

    public MethodWriterBuilder<T> proxyClass(Class<?> proxyClass) {
        if (proxyClass.isInterface())
            throw new IllegalArgumentException("expecting a class rather than an interface, proxyClass=" + proxyClass);
        this.proxyClass = proxyClass;
        return this;
    }

    @NotNull
    private String toFirstCapCase(@NotNull String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase();
    }

    /**
     * throws AbortCallingProxyException if the updateInterceptor returns {@code false}
     */
    static final class CallSupplierInvocationHandler implements InvocationHandler {

        private final UpdateInterceptor updateInterceptor;
        private final MethodWriterInvocationHandlerSupplier handlerSupplier;

        CallSupplierInvocationHandler(@NotNull final VanillaMethodWriterBuilder builder) {
            // Take a snapshot of these values so the builder can be reclaimed by the GC later
            this.updateInterceptor = builder.updateInterceptor;
            this.handlerSupplier = builder.handlerSupplier;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object args0 = args == null ? null : args[args.length - 1];
            return updateInterceptor == null || updateInterceptor.update(method.getName(), args0)
                    ? handlerSupplier.get().invoke(proxy, method, args)
                    : proxy;
        }
    }
}
