/*
 * Copyright 2016 higherfrequencytrading.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.Closeable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;


@SuppressWarnings({"rawtypes", "unchecked"})
public class VanillaMethodWriterBuilder<T> implements Supplier<T>, MethodWriterBuilder<T> {

    private static final boolean DISABLE_PROXY_GEN = Boolean.getBoolean("disableProxyCodegen");
    private static final Class<?> COMPILE_FAILED = ClassNotFoundException.class;
    private final Set<Class> interfaces = Collections.synchronizedSet(new LinkedHashSet<>());
    private static final Map<Set<Class>, Class> setOfClassesToClassName = new ConcurrentHashMap<>();
    //   private static final Map<String, Object> objectPool = new ConcurrentHashMap<>();

    private final String packageName;
    private final String className;
    private ClassLoader classLoader;
    @NotNull
    private final MethodWriterInvocationHandlerSupplier handlerSupplier;
    AtomicLong l = new AtomicLong();
    private String proxyClassName;
    private MarshallableOut out;
    private Closeable closeable;
    private String genericEvent;
    private MethodWriterListener methodWriterListener;
    private boolean metaData;
    private boolean useMethodIds;
    private WireType wireType;

    @NotNull
    public MethodWriterBuilder<T> classLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
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

    @NotNull
    public MethodWriterBuilder<T> recordHistory(boolean recordHistory) {
        handlerSupplier.recordHistory(recordHistory);
        return this;
    }

    public VanillaMethodWriterBuilder(@NotNull Class<T> tClass, @NotNull Supplier<MethodWriterInvocationHandler> handlerSupplier) {
        packageName = tClass.getPackage().getName();
        className = tClass.getSimpleName();

        addInterface(tClass);
        classLoader = tClass.getClassLoader();

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
        handlerSupplier.methodWriterListener(methodWriterListener);
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

    private Class<?> proxyClass;

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

    private Class generatedProxyClass(Set<Class> interfaces) {
        return GeneratedProxyClass.from(packageName, interfaces, proxyClassName, classLoader);
    }

    @NotNull
    @Override
    public T get() {

        proxyClassName = className + toFirstCapCase(wireType.toString()) + "MethodWriter";
        try {
            proxyClass = Class.forName(packageName + "." + proxyClassName + System.nanoTime());// l.incrementAndGet());
        } catch (ClassNotFoundException e) {
            // ignored
        }

        if (proxyClass != null) {
            try {
                Constructor<T> constructor = (Constructor) proxyClass.getConstructor(MethodWriterInvocationHandlerSupplier.class);
                return (T) constructor.newInstance(handlerSupplier);
            } catch (Throwable e) {
                // do nothing and drop through
                if (Jvm.isDebug())
                    Jvm.debug().on(getClass(), e);
            }
        }
        if
        (!DISABLE_PROXY_GEN) {

            try {

                interfaces.add(SharedDocumentContext.class);
                //  final Class<T> clazz = setOfClassesToClassName.computeIfAbsent(setOfInterfaces,
                //          i ->
                Class clazz = GenerateMethodWriter.from(packageName, interfaces, proxyClassName + System.nanoTime(), classLoader, wireType, genericEvent, methodWriterListener != null, metaData, useMethodIds);
                if (clazz != null)
                    return (T) newInstance3(clazz);

            } catch (Throwable e) {

                e.printStackTrace();

                // do nothing and drop through
                if (Jvm.isDebug())
                    Jvm.debug().on(getClass(), e);
            }
            final LinkedHashSet<Class> setOfInterfaces = new LinkedHashSet<>(interfaces);
            try {

                // this will create proxy that does not suffer from the arg[] issue
                //  setOfInterfaces.add(Closeable.class);
                setOfInterfaces.add(Closeable.class);
                final Class<T> o = setOfClassesToClassName.computeIfAbsent(setOfInterfaces, this::generatedProxyClass);
                if (o != null && o != COMPILE_FAILED)
                    return o.getConstructor(MethodWriterInvocationHandlerSupplier.class)
                            .newInstance(handlerSupplier);

            } catch (Throwable e) {
                setOfClassesToClassName.put(setOfInterfaces, COMPILE_FAILED);

                // do nothing and drop through
                if (Jvm.isDebug())
                    Jvm.debug().on(getClass(), e);
            }
        }

        @NotNull Class[] interfacesArr = interfaces.toArray(new Class[interfaces.size()]);
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(classLoader, interfacesArr, new CallSupplierInvocationHandler());
    }

    private Object newInstance3(final Class aClass) {
        try {
            return aClass.getDeclaredConstructors()[0].newInstance(out, closeable, methodWriterListener);
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

    public MethodWriterBuilder<T> marshallableOut(final MarshallableOut out) {
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
