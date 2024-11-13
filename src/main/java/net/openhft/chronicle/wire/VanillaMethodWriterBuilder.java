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
import net.openhft.chronicle.wire.internal.GenericReflection;
import net.openhft.chronicle.wire.internal.MethodWriterClassNameGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * This is the VanillaMethodWriterBuilder class implementing both Builder and MethodWriterBuilder interfaces.
 * It is responsible for constructing method writers based on specified configurations and properties.
 * The class has been designed to support a variety of functionalities like code generation disabling, proxy generation,
 * and method invocation handling among others.
 */
@SuppressWarnings({"rawtypes", "unchecked", "this-escape"})
public class VanillaMethodWriterBuilder<T> implements Builder<T>, MethodWriterBuilder<T> {
    // Flag name to check whether proxy code generation is disabled
    public static final String DISABLE_WRITER_PROXY_CODEGEN = "disableProxyCodegen";

    // Marker to indicate compilation failure
    private static final Class<?> COMPILE_FAILED = ClassNotFoundException.class;
    // Cache to store generated classes for reuse
    private static final Map<String, Class> classCache = new ConcurrentHashMap<>();
    // List of interfaces which are deemed unsuitable for super interfaces
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
     * Constructs an instance of VanillaMethodWriterBuilder with the specified class type, wire type,
     * and an invocation handler supplier.
     *
     * @param tClass The class type that the builder will be working on.
     * @param wireType The wire type to be used for the method writer.
     * @param handlerSupplier Supplier to provide invocation handlers for the method writer.
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
     * Configures the class loader to be used for dynamic class generation and loading.
     *
     * @param classLoader The class loader to be set.
     * @return The current instance of VanillaMethodWriterBuilder for chaining method calls.
     */
    @NotNull
    public MethodWriterBuilder<T> classLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    /**
     * Specifies if verbose types should be used during method writing.
     *
     * @return The current instance of VanillaMethodWriterBuilder for chaining method calls.
     */
    @Override
    @NotNull
    public MethodWriterBuilder<T> updateInterceptor(UpdateInterceptor updateInterceptor) {
        this.updateInterceptor = updateInterceptor;
        return this;
    }

    @NotNull
    public MethodWriterBuilder<T> verboseTypes(boolean verboseTypes) {
        this.verboseTypes = verboseTypes;
        return this;
    }

    /**
     * Adds an interface to the set of interfaces managed by this builder.
     * This method ensures that the provided interface does not violate any constraints
     * and adds it to the internal collection. Additionally, it recursively processes return
     * types of the methods in the provided interface and adds them if they are also interfaces.
     *
     * @param additionalClass The interface to be added.
     * @return The current instance of VanillaMethodWriterBuilder for chaining method calls.
     * @throws IllegalArgumentException if the provided interface is deemed invalid.
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
        for (Method method : additionalClass.getMethods()) {
            Type returnType = GenericReflection.getReturnType(method, additionalClass);
            if (!(returnType instanceof Class))
                continue;
            Class returnClass = (Class<?>) returnType;
            if (returnClass.isInterface() && !Jvm.dontChain(returnClass))
                addInterface(returnClass);
        }
        return this;
    }

    /**
     * Configures the thread-safety for the method writer invocation handler.
     * If thread-safety is disabled, this method will adjust the handler's behavior
     * to not be thread-safe. Otherwise, it will be thread-safe by default.
     *
     * @return The current instance of VanillaMethodWriterBuilder for chaining method calls.
     */
    @NotNull
    public MethodWriterBuilder<T> disableThreadSafe(boolean theadSafe) {
        handlerSupplier.disableThreadSafe(theadSafe);
        return this;
    }

    /**
     * Constructs and returns the method writer object based on the configurations set.
     *
     * @return A newly constructed method writer of type T.
     */
    @NotNull
    public T build() {
        return get();
    }

    /**
     * Registers a closeable resource with the method writer invocation handler.
     * This closeable will be invoked when the handler's close method is called.
     *
     * @param closeable The closeable resource to be registered.
     * @return The current instance of VanillaMethodWriterBuilder for chaining method calls.
     */
    @NotNull
    public MethodWriterBuilder<T> onClose(Closeable closeable) {
        this.closeable = closeable;
        handlerSupplier.onClose(closeable);
        return this;
    }

    /**
     * Fetches the wire type configuration set for the method writer.
     *
     * @return The current wire type.
     */
    public WireType wireType() {
        return wireType;
    }

    /**
     * Configures the wire type for the method writer.
     *
     * @param wireType The wire type to be set.
     * @return The current instance of VanillaMethodWriterBuilder for chaining method calls.
     */
    public VanillaMethodWriterBuilder<T> wireType(final WireType wireType) {
        this.wireType = wireType;
        return this;
    }

    /**
     * because we cache the classes in {@code classCache}, it's very important to come up with a name that is unique for what the class does.
     *
     * @return the name of the new class
     */
    @NotNull
    private String getClassName() {

        return methodWriterClassNameGenerator.getClassName(interfaces, genericEvent, metaData, updateInterceptor != null, wireType(), verboseTypes);

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
     * Attempts to create a new instance of the method writer by compiling or fetching the
     * appropriate class from cache, and then instantiating it.
     * <p>
     * First, the method tries to fetch the class by name. If the class is not found,
     * it attempts to generate a new class. In case of a failure during the class generation,
     * a warning is logged, and a proxy method writer is used as a fallback.
     *
     * @return A newly created instance of the method writer or {@code null} if the instance couldn't be created.
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
     * Generates a new method writer class with the given fully qualified class name. Depending on
     * the wire type and system settings, either the version 1 or version 2 of the method writer
     * generator is used to create the class.
     * <p>
     * The method configures the class generator with various settings, such as package name,
     * base class name, interfaces, event types, and other configuration parameters.
     *
     * @param fullClassName The fully qualified name of the class to be generated.
     * @return The generated class, or {@code COMPILE_FAILED} if class generation failed.
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
     * Creates a new instance of the given class. The expected class should have a constructor
     * that takes in an outSupplier, a closeable, and an updateInterceptor.
     * <p>
     * Before the instantiation, it checks if the outSupplier is set and whether it records
     * history. If the outSupplier does record history, it enables recordHistory for the
     * handlerSupplier as well.
     *
     * @param aClass The class for which a new instance is to be created.
     * @return A newly created object of the provided class.
     * @throws NullPointerException if the outSupplier is not set.
     * @throws RuntimeException if any other exception occurs during instantiation.
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

    /**
     * Sets the {@link MarshallableOut} instance to be used by the builder.
     * This will internally set a supplier that always returns the given instance.
     *
     * @param out The instance of {@link MarshallableOut} to be set.
     * @return The current instance of the {@link MethodWriterBuilder}, allowing chained method calls.
     */
    public MethodWriterBuilder<T> marshallableOut(@NotNull final MarshallableOut out) {
        this.outSupplier = () -> out;
        return this;
    }

    /**
     * Sets the supplier for the {@link MarshallableOut} to be used by the builder.
     *
     * @param out The supplier of {@link MarshallableOut} to be set.
     * @return The current instance of the {@link MethodWriterBuilder}, allowing chained method calls.
     */
    public MethodWriterBuilder<T> marshallableOutSupplier(@NotNull final Supplier<MarshallableOut> out) {
        this.outSupplier = out;
        return this;
    }

    /**
     * Specifies whether the builder should include metadata or not.
     *
     * @param metaData A boolean indicating whether to include metadata.
     * @return The current instance of the {@link MethodWriterBuilder}, allowing chained method calls.
     */
    @Override
    public MethodWriterBuilder<T> metaData(final boolean metaData) {
        this.metaData = metaData;
        return this;
    }

    /**
     * Retrieves the proxy class being used by the builder.
     *
     * @return The current proxy class.
     */
    public Class<?> proxyClass() {
        return proxyClass;
    }

    /**
     * Sets the proxy class to be used by the builder. The provided class must not be an interface.
     *
     * @param proxyClass The class to be set as the proxy class.
     * @return The current instance of the {@link MethodWriterBuilder}, allowing chained method calls.
     * @throws IllegalArgumentException If the provided class is an interface.
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
