/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.MethodReaderBuilder;
import net.openhft.chronicle.bytes.MethodReaderInterceptorReturns;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.onoes.ExceptionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static net.openhft.chronicle.wire.WireParser.SKIP_READABLE_BYTES;

/**
 * Builder for {@link VanillaMethodReader}. Implements {@link MethodReaderBuilder} and
 * allows configuration of default parsing behaviour, exception handling, method
 * interception and use of generated proxies.
 */
public class VanillaMethodReaderBuilder implements MethodReaderBuilder {

    // System property name to disable proxy generation.
    public static final String DISABLE_READER_PROXY_CODEGEN = "disableReaderProxyCodegen";

    // Cache of generated reader classes keyed by name.
    private static final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();

    // Marker stored in {@link #classCache} when generation fails.
    private static final Class<?> COMPILE_FAILED = ClassNotFoundException.class;

    // The input from which method calls are read.
    private final MarshallableIn in;

    // A flag to determine whether default values should be ignored.
    private boolean ignoreDefaults;

    // The default parselet used when a method is not recognized.
    private WireParselet defaultParselet;

    // An interceptor for handling return values from method calls.
    private MethodReaderInterceptorReturns methodReaderInterceptorReturns;

    // The wire type for deserialization.
    private WireType wireType;

    // An optional handler for metadata associated with method calls.
    private Object[] metaDataHandler = null;

    // The exception handler to use when a method is not recognized.
    private ExceptionHandler exceptionHandlerOnUnknownMethod = Jvm.getBoolean("chronicle.methodReader.warn") ? Jvm.warn() : Jvm.debug();

    // A predicate to further filter method calls.
    private Predicate<MethodReader> predicate = x -> true;

    // A flag to indicate whether the reader is in a scanning mode.
    private boolean scanning = false;

    // A flag to determine support for parameters which can either be non-Marshallable or Marshallable
    // null for auto-detect
    private Boolean multipleNonMarshallableParamTypes = null;

    /**
     * Constructs a new {@code VanillaMethodReaderBuilder} with the specified wire input.
     *
     * @param in The input from which method calls are read.
     */
    public VanillaMethodReaderBuilder(MarshallableIn in) {
        this.in = in;
    }

    /**
     * Creates a default {@link WireParselet} that handles unrecognized methods.
     * When an unrecognized method is encountered, it logs a warning or uses
     * the provided exception handler, depending on the method name's length.
     *
     * @param exceptionHandlerOnUnknownMethod The exception handler to use when a method is not recognized.
     * @return A {@link WireParselet} that logs or handles unrecognized methods.
     */
    @Deprecated(/* to be removed in x.29 */)
    @NotNull
    public static WireParselet createDefaultParselet(ExceptionHandler exceptionHandlerOnUnknownMethod) {
        return createDefaultParselet(exceptionHandlerOnUnknownMethod, null);
    }

    /**
     * Creates a default {@link WireParselet} that handles unrecognized methods.
     * When an unrecognized method is encountered, it logs a warning or uses
     * the provided exception handler, depending on the method name's length.
     *
     * @param exceptionHandlerOnUnknownMethod The exception handler to use when a method is not recognized.
     * @param clazz The class on which the method was called, used for logging purposes.
     * @return A {@link WireParselet} that logs or handles unrecognized methods.
     */
    public static WireParselet createDefaultParselet(ExceptionHandler exceptionHandlerOnUnknownMethod, Class<?> clazz) {
        return (s, v) -> {
            MessageHistory history = MessageHistory.get();
            long sourceIndex = history.lastSourceIndex();
            v.skipValue();
            ExceptionHandler eh = s.length() == 0
                    ? Jvm.warn()
                    : exceptionHandlerOnUnknownMethod;
            if (eh.isEnabled(VanillaMethodReader.class)) {
                eh.on(VanillaMethodReader.class, errorMsg(s, history, sourceIndex, clazz));
            }
        };
    }

    /**
     * Returns an error message based on the given parameters.
     *
     * @param s           The sequence that represents either a method name or a method ID.
     * @param history     The message history for the current method call.
     * @param sourceIndex The index of the source from where the method call was read.
     * @param clazz       The class on which the method was called, used for logging purposes.
     * @return A formatted error message for unrecognized methods or method IDs.
     */
    @NotNull
    private static String errorMsg(CharSequence s, MessageHistory history, long sourceIndex, @Nullable Class<?> clazz) {

        // Determine whether the provided sequence is a method name or a method ID based on its first character.
        final String identifierType = s.length() != 0 && Character.isDigit(s.charAt(0)) ? "@MethodId" : "method-name";
        StringBuilder msg = new StringBuilder()
                .append("Unknown ").append(identifierType)
                .append("='").append(s).append("'");
        if (clazz != null) {
            if (clazz.getName().contains("Proxy$") || clazz.getName().contains("$Lambda"))
                clazz = clazz.getInterfaces()[0];
            msg.append(" called on ").append(clazz);
        }
        if (history.lastSourceId() >= 0)
            msg.append(" from ").append(history.lastSourceId())
                    .append(" at ").append(Long.toHexString(sourceIndex))
                    .append(" ~ ").append((int) sourceIndex);
        return msg.toString();
    }

    public WireParselet defaultParselet() {
        return defaultParselet;
    }

    /**
     * Sets a new default parselet.
     *
     * @param defaultParselet The new default parselet.
     * @return This builder for chaining.
     */
    public MethodReaderBuilder defaultParselet(WireParselet defaultParselet) {
        this.defaultParselet = defaultParselet;
        return this;
    }

    /**
     * Sets the method reader interceptor that handles return values from method calls.
     *
     * @param methodReaderInterceptorReturns The interceptor to set.
     * @return This builder for chaining.
     */
    public VanillaMethodReaderBuilder methodReaderInterceptorReturns(MethodReaderInterceptorReturns methodReaderInterceptorReturns) {
        this.methodReaderInterceptorReturns = methodReaderInterceptorReturns;
        return this;
    }

    @Override
    public MethodReaderBuilder exceptionHandlerOnUnknownMethod(ExceptionHandler exceptionHandler) {
        this.exceptionHandlerOnUnknownMethod = exceptionHandler;
        return this;
    }

    /**
     * Returns the {@code wireType} of the reader.
     *
     * @return The {@code wireType} of the reader.
     */
    public WireType wireType() {
        return wireType;
    }

    /**
     * Sets the {@code wireType} of the reader.
     *
     * @param wireType The {@code wireType} to set.
     * @return This builder instance for chaining.
     */
    public VanillaMethodReaderBuilder wireType(WireType wireType) {
        this.wireType = wireType;
        return this;
    }

    /**
     * Configures the reader to skip over metadata and unknown events to find at least one known event.
     * This is useful when reading from a stream with mixed types of data.
     *
     * @param scanning Whether the reader should skip to the next known event.
     * @return This builder instance for chaining.
     */
    public VanillaMethodReaderBuilder scanning(boolean scanning) {
        this.scanning = scanning;
        return this;
    }

    /**
     * Configures the reader to handle parameters which can have multiple non-marshallable parameter types.
     *
     * @param multipleNonMarshallableParamTypes Whether the reader should handle multiple non-marshallable parameter types.
     * @return This builder instance for chaining.
     */
    public VanillaMethodReaderBuilder multipleNonMarshallableParamTypes(Boolean multipleNonMarshallableParamTypes) {
        this.multipleNonMarshallableParamTypes = multipleNonMarshallableParamTypes;
        return this;
    }

    /**
     * Creates an instance of a generated method reader.
     * The method first checks if the desired generated reader class is already loaded.
     * If not, it attempts to generate a new class and then instantiate it.
     *
     * @param impls An array of implementations used by the generated method reader.
     * @return An instance of the generated method reader or null if the class generation failed.
     */
    @Nullable
    private MethodReader createGeneratedInstance(Object... impls) {
        if (ignoreDefaults || Jvm.getBoolean(DISABLE_READER_PROXY_CODEGEN))
            return null;

        GenerateMethodReader generateMethodReader = new GenerateMethodReader(wireType, methodReaderInterceptorReturns, multipleNonMarshallableParamTypes, metaDataHandler, impls);

        String fullClassName = generateMethodReader.packageName() + "." + generateMethodReader.generatedClassName();

        try {
            try {
                final Class<?> generatedClass = Class.forName(fullClassName);

                return instanceForGeneratedClass(generatedClass, impls);
            } catch (ClassNotFoundException e) {
                Class<?> clazz = classCache.computeIfAbsent(fullClassName, name -> generateMethodReader.createClass());
                if (clazz != null && clazz != COMPILE_FAILED) {
                    return instanceForGeneratedClass(clazz, impls);
                }
            }
        } catch (Throwable e) {
            classCache.put(fullClassName, COMPILE_FAILED);
            Jvm.warn().on(getClass(), "Failed to compile generated method reader - " +
                    "falling back to proxy method reader. Please report this failure as support for " +
                    "proxy method readers will be dropped in x.25.", e);
        }

        return null;
    }

    /**
     * Instantiates a reader from a generated class.
     * This method handles creating an instance for classes generated at runtime.
     *
     * @param generatedClass The class of the generated method reader.
     * @param impls An array of implementations used by the generated method reader.
     * @return An instance of the generated method reader.
     * @throws InstantiationException If the class cannot be instantiated.
     * @throws IllegalAccessException If the constructor or a method is not accessible.
     * @throws InvocationTargetException If a method throws an exception.
     */
    @NotNull
    private MethodReader instanceForGeneratedClass(Class<?> generatedClass, Object[] impls)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        final Constructor<?> constructor = generatedClass.getConstructors()[0];

        WireParselet debugLoggingParselet = VanillaMethodReader::logMessage;

        MethodReader reader = (MethodReader) constructor.newInstance(
                in, defaultParselet, debugLoggingParselet, methodReaderInterceptorReturns, metaDataHandler,
                impls);
        if (reader instanceof AbstractGeneratedMethodReader) {
            AbstractGeneratedMethodReader reader0 = (AbstractGeneratedMethodReader) reader;
            reader0.scanning(scanning);
            reader0.predicate(predicate);
        }

        return reader;
    }

    @Override
    public MethodReaderBuilder metaDataHandler(Object... components) {
        this.metaDataHandler = components;
        return this;
    }

    /**
     * Constructs and returns a {@code MethodReader} instance using the given implementations.
     * If the generated reader instance is not available, it falls back to a default implementation.
     *
     * @param impls An array of implementations used by the method reader.
     * @return A built {@code MethodReader} instance.
     */
    @NotNull
    public MethodReader build(Object... impls) {
        if (this.defaultParselet == null)
            this.defaultParselet = createDefaultParselet(exceptionHandlerOnUnknownMethod, impls.length > 0 ? impls[0].getClass() : null);

        @SuppressWarnings("resource")
        final MethodReader generatedInstance = createGeneratedInstance(impls);

        // If the generated instance isn't available, use the default vanilla method reader.
        return generatedInstance == null ? new VanillaMethodReader(in, ignoreDefaults, defaultParselet, SKIP_READABLE_BYTES,
                methodReaderInterceptorReturns, metaDataHandler, predicate,
                impls) :
                generatedInstance;
    }

    @Override
    public MethodReaderBuilder predicate(Predicate<MethodReader> predicate) {
        this.predicate = predicate;
        return this;
    }
}
