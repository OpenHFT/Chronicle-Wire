/*
 * Copyright 2016-2020 Chronicle Software
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@SuppressWarnings({"rawtypes", "unchecked"})
public class VanillaMethodWriterBuilder<T> implements Supplier<T>, MethodWriterBuilder<T> {

    private static final boolean DISABLE_PROXY_GEN = Jvm.getBoolean("disableProxyCodegen", false);
    private static final Class<?> COMPILE_FAILED = ClassNotFoundException.class;
    private static final Map<String, Class> classCache = new ConcurrentHashMap<>();
    private final Set<Class> interfaces = Collections.synchronizedSet(new LinkedHashSet<>());

    private final String packageName;
    private ClassLoader classLoader;
    @NotNull
    private final MethodWriterInvocationHandlerSupplier<T> handlerSupplier;
    private MarshallableOut out;
    private Closeable closeable;
    private String genericEvent;
    private MethodWriterListener methodWriterListener;
    private boolean metaData;
    private boolean useMethodIds;
    private WireType wireType;
    private Class<?> proxyClass;
    private UpdateInterceptor updateInterceptor;

    @NotNull
    public MethodWriterBuilder<T> classLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    /**
     * @param updateInterceptor used to modifier the the data before it is written to the wire
     */
    @NotNull
    public MethodWriterBuilder<T> updateInterceptor(UpdateInterceptor updateInterceptor) {
        this.updateInterceptor = updateInterceptor;
        return this;
    }

    @NotNull
    public MethodWriterBuilder<T> addInterface(Class additionalClass) {
        if (interfaces.contains(additionalClass))
            return this;

        interfaces.add(additionalClass);
        for (Method method : additionalClass.getMethods()) {
            Class<?> returnType = method.getReturnType();
            if (returnType.isInterface() && !Jvm.dontChain(returnType)) {
                addInterface(returnType);
            }
        }
        return this;
    }

    // sourceId enables this, this isn't useful unless it's set.
    @Deprecated
    @NotNull
    public MethodWriterBuilder<T> recordHistory(boolean recordHistory) {
        handlerSupplier.recordHistory(recordHistory);
        return this;
    }

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
    public MethodWriterBuilder<T> methodWriterInterceptor(MethodWriterInterceptor methodWriterInterceptor) {
        handlerSupplier.methodWriterInterceptor(methodWriterInterceptor);
        return this;
    }

    @NotNull
    public MethodWriterBuilder<T> methodWriterInterceptorReturns(MethodWriterInterceptorReturns methodWriterInterceptor) {
        handlerSupplier.methodWriterInterceptorReturns(methodWriterInterceptor);
        return this;
    }

    @NotNull
    public MethodWriterBuilder<T> methodWriterListener(MethodWriterListener methodWriterListener) {
        this.methodWriterListener = methodWriterListener;
        this.handlerSupplier.methodWriterListener(methodWriterListener);
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
        interfaces.forEach(i -> sb.append(i.getSimpleName()));
        sb.append(this.genericEvent == null ? "" : this.genericEvent);
        sb.append(this.metaData ? "MetadataAware" : "");
        sb.append(useMethodIds ? "MethodIds" : "");
        sb.append(updateInterceptor != null ? "Intercepting" : "");
        sb.append(hasMethodWriterListener() ? "MethodListener" : "");
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
                if (Jvm.isDebug())
                    Jvm.debug().on(getClass(), e);
            }
        }
        if (!DISABLE_PROXY_GEN && handlerSupplier.methodWriterInterceptorReturns() == null) {
            T t = createInstance();
            if (t != null)
                return t;
        }

        @NotNull Class[] interfacesArr = interfaces.toArray(new Class[interfaces.size()]);

        //noinspection unchecked
        return (T) Proxy.newProxyInstance(classLoader, interfacesArr, new CallSupplierInvocationHandler());
    }

    @Nullable
    private T createInstance() {
        String className = getClassName();
        try {
            try {
                return (T) newInstance(Class.forName(className));
            } catch (ClassNotFoundException e) {
                Class clazz = classCache.computeIfAbsent(className, name -> newClass(name));
                if (clazz != null && clazz != COMPILE_FAILED) {
                    return (T) newInstance(clazz);
                }
            }
        } catch (Throwable e) {
            classCache.put(className, COMPILE_FAILED);
            // do nothing and drop through
            if (Jvm.isDebug())
                Jvm.debug().on(getClass(), e);
        }
        return null;
    }

    private Class newClass(final String name) {
        final LinkedHashSet<Class> setOfInterfaces = new LinkedHashSet<>(interfaces);
        setOfInterfaces.add(SharedDocumentContext.class);
        return GenerateMethodWriter.newClass(packageName,
                setOfInterfaces,
                name,
                classLoader,
                wireType,
                genericEvent,
                hasMethodWriterListener(),
                metaData,
                useMethodIds, updateInterceptor != null);
    }

    private boolean hasMethodWriterListener() {
        return methodWriterListener != null;
    }

    private Object newInstance(final Class aClass) {
        try {
            if (out == null)
                throw new NullPointerException("marshallableOut(out) has not been set.");
            if (out.recordHistory()) {
                recordHistory(true);
                handlerSupplier.recordHistory(true);
            }
            return aClass.getDeclaredConstructors()[0].newInstance(out, closeable, methodWriterListener, updateInterceptor);
        } catch (Exception e) {
            throw Jvm.rethrow(e);
        }
    }

    /**
     * A generic event treats the first argument and the eventName
     *
     * @param genericEvent name
     * @return this
     */
    public MethodWriterBuilder<T> genericEvent(String genericEvent) {
        handlerSupplier.genericEvent(genericEvent);
        this.genericEvent = genericEvent;
        return this;
    }

    public MethodWriterBuilder<T> useMethodIds(boolean useMethodIds) {
        handlerSupplier.useMethodIds(useMethodIds);
        this.useMethodIds = useMethodIds;
        return this;
    }

    public MethodWriterBuilder<T> marshallableOut(@NotNull final MarshallableOut out) {
        this.out = out;
        return this;
    }

    public MethodWriterBuilder<T> metaData(final boolean metaData) {
        this.metaData = metaData;
        return this;
    }

    class CallSupplierInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return handlerSupplier.get().invoke(proxy, method, args);
        }
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
}
