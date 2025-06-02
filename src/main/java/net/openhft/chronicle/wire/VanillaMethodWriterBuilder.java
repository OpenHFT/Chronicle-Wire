/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
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
import net.openhft.chronicle.core.util.GenericReflection;
import net.openhft.chronicle.wire.internal.MethodWriterClassNameGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Builder for dynamic proxies that write method calls to a {@link MarshallableOut}.
 *
 * <p>The writer attempts to generate and compile a dedicated implementation for
 * the configured interfaces. If generation is disabled or fails it falls back to
 * a standard {@link Proxy}.
 *
 * <p>Options include additional interfaces, generic event handling and update
 * interceptors. By default a thread-local invocation handler is used but this
 * can be overridden via {@link #disableThreadSafe(boolean)}.
 *
 * @see MethodWriter
 * @see MarshallableOut#methodWriterBuilder(Class)
 */
@SuppressWarnings({"rawtypes", "unchecked", "this-escape"})
public class VanillaMethodWriterBuilder<T> implements Builder<T>, MethodWriterBuilder<T> {
    /** System property to disable byte-code generation. */
    public static final String DISABLE_WRITER_PROXY_CODEGEN = "disableProxyCodegen";

    /** Marker inserted into {@link #classCache} when compilation fails. */
    private static final Class<?> COMPILE_FAILED = ClassNotFoundException.class;

    /** Cache of generated writer classes keyed by name. */
    private static final Map<String, Class> classCache = new ConcurrentHashMap<>();

    /** Interfaces that must not be implemented by writer proxies. */
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
            Comparator.class
    );

    // Flag to indicate if the proxy generation is disabled
    private final boolean disableProxyGen = Jvm.getBoolean(DISABLE_WRITER_PROXY_CODEGEN, false);
    // A synchronized set of classes to represent interfaces
    private final Set<Class<?>> interfaces = Collections.synchronizedSet(new LinkedHashSet<>());

    // Instance responsible for generating class names for method writers
    private final MethodWriterClassNameGenerator methodWriterClassNameGenerator;
    // Package name where the class will reside
    private final String packageName;
    // Class loader to be used for dynamic class generation and loading
    private ClassLoader classLoader;
    // Supplier that provides a MethodWriterInvocationHandler for proxy method calls
    @NotNull
    private final MethodWriterInvocationHandlerSupplier handlerSupplier;
    // Supplier to get an instance of MarshallableOut
    private Supplier<MarshallableOut> outSupplier;
    // A Closeable resource associated with the builder
    private Closeable closeable;
    // Name of the generic event
    private String genericEvent;
    // Flag to indicate if meta-data should be used
    private boolean metaData;
    // Specifies the wire type to be used
    private WireType wireType;
    // The dynamically created proxy class
    private Class<?> proxyClass;
    // An interceptor that is triggered on updates
    private UpdateInterceptor updateInterceptor;
    // Flag to indicate if verbose types should be used
    private boolean verboseTypes;

    /**
     * @param tClass          primary interface to implement
     * @param wireType        associated {@link WireType}
     * @param handlerSupplier supplies the {@link MethodWriterInvocationHandler}
     *                        used to process method calls
     */
    public VanillaMethodWriterBuilder(@NotNull Class<T> tClass,
                                      WireType wireType,
                                      @NotNull Supplier<MethodWriterInvocationHandler> handlerSupplier) {
        this.packageName = Jvm.getPackageName(tClass);
        this.wireType = wireType;
        addInterface(tClass);
        ClassLoader clsLdr = tClass.getClassLoader();
        // TODO Using loader of parent class may not be safe if it's not accepting new classes.
        //  Maybe have an option to always use current thread class loader?
        this.classLoader = clsLdr != null ? clsLdr : getClass().getClassLoader();
        this.methodWriterClassNameGenerator = new MethodWriterClassNameGenerator();
        this.handlerSupplier = new MethodWriterInvocationHandlerSupplier(handlerSupplier);
    }

    /**
     * Uses the supplied class loader when defining generated classes.
     *
     * @return this builder for chaining
     */
    @NotNull
    public MethodWriterBuilder<T> classLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    /**
     * Sets an {@link UpdateInterceptor} to be invoked before each method call.
     * The interceptor may veto the call by returning {@code false}.
     *
     * @return this builder
     */
    @Override
    @NotNull
    public MethodWriterBuilder<T> updateInterceptor(UpdateInterceptor updateInterceptor) {
        this.updateInterceptor = updateInterceptor;
        return this;
    }

    /**
     * @return this builder after setting whether type names should be written verbosely
     */
    @NotNull
    public MethodWriterBuilder<T> verboseTypes(boolean verboseTypes) {
        this.verboseTypes = verboseTypes;
        return this;
    }

    /**
     * Adds an additional interface that the writer proxy should implement. The
     * interface must not belong to {@link #invalidSuperInterfaces}.
     * Any non standard interface return types are also added recursively.
     *
     * @param additionalClass interface to add
     * @return this builder
     * @throws IllegalArgumentException if {@code additionalClass} is not allowed
     */
    @NotNull
    public MethodWriterBuilder<T> addInterface(Class<?> additionalClass) {
        if (interfaces.contains(additionalClass))
            return this;
        if (additionalClass == DocumentContext.class)
            return this;

        for (Class<?> invalidSuperInterface : invalidSuperInterfaces) {
            if (invalidSuperInterface.isAssignableFrom(additionalClass))
                throw new IllegalArgumentException("The event interface shouldn't implement " + invalidSuperInterface.getName());
        }
        interfaces.add(additionalClass);
        for (Type returnType : GenericReflection.getMethodReturnTypes(additionalClass)) {
            if (!(returnType instanceof Class))
                continue;
            Class returnClass = (Class<?>) returnType;
            if (returnClass.isInterface() && !Jvm.dontChain(returnClass))
                addInterface(returnClass);
        }
        return this;
    }

    /**
     * Controls whether the same invocation handler instance can be reused across threads.
     * When {@code true} a single non-thread-safe handler may be shared.
     *
     * @return this builder
     */
    @NotNull
    public MethodWriterBuilder<T> disableThreadSafe(boolean theadSafe) {
        handlerSupplier.disableThreadSafe(theadSafe);
        return this;
    }

    /**
     * Builds the method writer proxy.
     * Tries to use a compiled implementation and falls back to a standard
     * {@link Proxy} if generation is disabled or fails.
     *
     * @return a new proxy implementing {@code T}
     * @throws NullPointerException if {@link #marshallableOut(MarshallableOut)} was not configured
     */
    @NotNull
    public T build() {
        return get();
    }

    /**
     * Adds a resource to close when the writer or its handler is closed.
     *
     * @return this builder
     */
    @NotNull
    public MethodWriterBuilder<T> onClose(Closeable closeable) {
        this.closeable = closeable;
        handlerSupplier.onClose(closeable);
        return this;
    }

    /**
     * @return current {@link WireType} for the writer
     */
    public WireType wireType() {
        return wireType;
    }

    /**
     * Sets the {@link WireType} used by the generated writer.
     *
     * @return this builder
     */
    public VanillaMethodWriterBuilder<T> wireType(final WireType wireType) {
        this.wireType = wireType;
        return this;
    }

    /**
     * Generates a unique class name for the proxy based on the configured options.
     */
    @NotNull
    private String getClassName() {

        return methodWriterClassNameGenerator.getClassName(interfaces, genericEvent, metaData, updateInterceptor != null, wireType(), verboseTypes);

    }

    /**
     * Returns the method writer proxy instance. Tries to reuse or generate a
     * compiled class before falling back to {@link Proxy}.
     */
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
        if (!disableProxyGen) {
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

    /**
     * Tries to instantiate a previously generated writer class, generating it if
     * necessary. Returns {@code null} when generation fails.
     */
    @Nullable
    private T createInstance() {
        String fullClassName = packageName + "." + getClassName();
        try {
            try {
                // Attempt to create an instance from an already loaded class
                return (T) newInstance(Class.forName(fullClassName));
            } catch (ClassNotFoundException e) {
                Class<?> clazz;
                // only one thread at a time so two threads don't try to generate the same class.
                synchronized (classCache) {
                    clazz = classCache.computeIfAbsent(fullClassName, this::newClass);
                }
                if (clazz != null && clazz != COMPILE_FAILED) {
                    return (T) newInstance(clazz);
                }
            }
        } catch (MethodWriterValidationException e) {
            throw e;
        } catch (Throwable e) {
            // Log the exception and fallback to proxy method writer
            classCache.put(fullClassName, COMPILE_FAILED);
            Jvm.warn().on(getClass(), "Failed to compile generated method writer - " +
                    "falling back to proxy method writer. Please report this failure as support for " +
                    "proxy method writers will be dropped in x.25.", e);
        }
        return null;
    }

    /**
     * Creates and compiles the writer class with the supplied name using the appropriate
     * code generator.
     */
    private Class<?> newClass(final String fullClassName) {
        if (wireType.isText() || !Jvm.getBoolean("wire.generator.v2"))
            // Use version 1 of the method writer generator
            return GenerateMethodWriter.newClass(fullClassName,
                    interfaces,
                    classLoader,
                    wireType,
                    genericEvent,
                    metaData,
                    true,
                    updateInterceptor != null, verboseTypes);

        // Configure and use version 2 of the method writer generator
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

    /**
     * Instantiates the generated writer class via its expected constructor.
     */
    private Object newInstance(final Class<?> aClass) {
        try {
            // Ensure the outSupplier is set before proceeding.
            if (outSupplier == null)
                throw new NullPointerException("marshallableOut(out) has not been set.");

            // Check if the outSupplier records history and enable it for the handlerSupplier if it does.
            if (outSupplier.get().recordHistory()) {
                handlerSupplier.recordHistory(true);
            }

            // Use the first declared constructor of the provided class to create a new instance.
            return aClass.getDeclaredConstructors()[0].newInstance(outSupplier, closeable, updateInterceptor);
        } catch (Exception e) {
            // Rethrow any exception that might occur during instantiation.
            throw Jvm.rethrow(e);
        }
    }

    /**
     * Treats calls to {@code genericEvent} specially, using the first argument
     * as the event name on the wire.
     *
     * @return this builder
     */
    public MethodWriterBuilder<T> genericEvent(String genericEvent) {
        handlerSupplier.genericEvent(genericEvent);
        this.genericEvent = genericEvent;
        return this;
    }

    /**
     * Sends method calls to the given {@link MarshallableOut}.
     */
    public MethodWriterBuilder<T> marshallableOut(@NotNull final MarshallableOut out) {
        this.outSupplier = () -> out;
        return this;
    }

    /**
     * Uses a supplier to obtain the {@link MarshallableOut} for each call.
     */
    public MethodWriterBuilder<T> marshallableOutSupplier(@NotNull final Supplier<MarshallableOut> out) {
        this.outSupplier = out;
        return this;
    }

    /**
     * Marks all written documents as meta-data when {@code true}.
     */
    @Override
    public MethodWriterBuilder<T> metaData(final boolean metaData) {
        this.metaData = metaData;
        return this;
    }

    /**
     * @return pre-compiled proxy class if set
     */
    public Class<?> proxyClass() {
        return proxyClass;
    }

    /**
     * Uses the supplied class instead of generating one.
     * The class must not be an interface.
     *
     * @return this builder
     */
    public MethodWriterBuilder<T> proxyClass(Class<?> proxyClass) {
        // Check if the provided class is an interface.
        if (proxyClass.isInterface())
            throw new IllegalArgumentException("expecting a class rather than an interface, proxyClass=" + proxyClass);
        this.proxyClass = proxyClass;
        return this;
    }

    /**
     * Converts the first character of a given string to uppercase and the rest to lowercase.
     *
     * @param name The input string to be converted.
     * @return The converted string with its first character in uppercase and the rest in lowercase.
     */
    @NotNull
    private String toFirstCapCase(@NotNull String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase();
    }

    /**
     * The {@code CallSupplierInvocationHandler} class is an implementation of {@link InvocationHandler}
     * designed to act as a proxy for method calls. If the associated {@link UpdateInterceptor} returns
     * {@code false}, an {@code AbortCallingProxyException} is thrown, indicating that the proxy
     * method invocation should be aborted.
     */
    static final class CallSupplierInvocationHandler implements InvocationHandler {

        private final UpdateInterceptor updateInterceptor;
        private final MethodWriterInvocationHandlerSupplier handlerSupplier;

        /**
         * Constructs a new {@code CallSupplierInvocationHandler} using the specified builder.
         * The values from the builder are captured in a snapshot so that the builder can be
         * garbage collected if no longer referenced.
         *
         * @param builder The {@link VanillaMethodWriterBuilder} instance to extract values from.
         */
        CallSupplierInvocationHandler(@NotNull final VanillaMethodWriterBuilder builder) {
            // Take a snapshot of these values so the builder can be reclaimed by the GC later.
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
