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

import net.openhft.chronicle.bytes.MethodWriterBuilder;
import net.openhft.chronicle.bytes.MethodWriterInterceptor;
import net.openhft.chronicle.bytes.MethodWriterInvocationHandler;
import net.openhft.chronicle.bytes.MethodWriterListener;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.Closeable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/*
 * Created by Peter Lawrey on 28/03/16.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class VanillaMethodWriterBuilder<T> implements Supplier<T>, MethodWriterBuilder<T> {

    private final List<Class> interfaces = new ArrayList<>();
    @NotNull
    private final MethodWriterInvocationHandler handler;
    private final String packageName;
    private ClassLoader classLoader;
    private static Map<Set<Class>, Class> setOfClassesToClassName = new ConcurrentHashMap<>();

    public VanillaMethodWriterBuilder(@NotNull Class<T> tClass, @NotNull MethodWriterInvocationHandler handler) {
        packageName = tClass.getPackage().getName();
        interfaces.add(Closeable.class);
        interfaces.add(tClass);
        classLoader = tClass.getClassLoader();
        this.handler = handler;
    }

    private static AtomicLong proxyCount = new AtomicLong();

    @NotNull
    public MethodWriterBuilder<T> classLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    @NotNull
    public MethodWriterBuilder<T> addInterface(Class additionalClass) {
        interfaces.add(additionalClass);
        return this;
    }

    @NotNull
    public MethodWriterBuilder<T> recordHistory(boolean recordHistory) {
        handler.recordHistory(recordHistory);
        return this;
    }

    @NotNull
    public MethodWriterBuilder<T> methodWriterListener(MethodWriterListener methodWriterListener) {
        handler.methodWriterListener(methodWriterListener);
        return this;
    }

    @NotNull
    public MethodWriterBuilder<T> methodWriterInterceptor(MethodWriterInterceptor methodWriterInterceptor) {
        handler.methodWriterInterceptor(methodWriterInterceptor);
        return this;
    }

    @NotNull
    public MethodWriterBuilder<T> onClose(Closeable closeable) {
        handler.onClose(closeable);
        return this;
    }

    @NotNull
    public T build() {
        return get();
    }

    private Class<?> proxyClass;

    private static <T> Class generatedProxyClass(String packageName, Set<Class> interfaces) {
        return GeneratedProxyClass.from(packageName, interfaces, "Proxy" + proxyCount.incrementAndGet());
    }

    @NotNull
    @Override
    public T get() {
        if (proxyClass != null) {

            try {
                Constructor<T> constructors = (Constructor) proxyClass.getConstructor(Object.class, InvocationHandler.class);

                @NotNull Class[] interfacesArr = interfaces.toArray(new Class[interfaces.size()]);
                //noinspection unchecked
                Object proxy = Proxy.newProxyInstance(classLoader, interfacesArr, handler);

                return (T) constructors.newInstance(proxy, handler);
            } catch (Throwable e) {
                // do nothing and drop through
                if (Jvm.isDebug())
                    Jvm.debug().on(getClass(), e);
            }
        }

        try {
            @NotNull Class[] interfacesArr = interfaces.toArray(new Class[interfaces.size()]);
            Object proxy = Proxy.newProxyInstance(classLoader, interfacesArr, handler);

            Set<Class> interfaces = new HashSet<>();
            Collections.addAll(interfaces, interfacesArr);

            // this will create proxy that does not suffer from the arg[] issue
            final Class<T> o = setOfClassesToClassName.computeIfAbsent(interfaces,
                    i -> VanillaMethodWriterBuilder.generatedProxyClass(packageName, i));
            if (o != null)
                return o.getConstructor(Object.class, InvocationHandler.class).newInstance(proxy, handler);
            
        } catch (Throwable e) {
            // do nothing and drop through
            if (Jvm.isDebug())
                Jvm.debug().on(getClass(), e);
        }

        @NotNull Class[] interfacesArr = interfaces.toArray(new Class[interfaces.size()]);
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(classLoader, interfacesArr, handler);
    }

    /**
     * A generic event treats the first argument and the eventName
     *
     * @param genericEvent name
     * @return this
     */
    public MethodWriterBuilder<T> genericEvent(String genericEvent) {
        handler.genericEvent(genericEvent);
        return this;
    }

    public MethodWriterBuilder<T> useMethodIds(boolean useMethodIds) {
        handler.useMethodIds(useMethodIds);
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

}
