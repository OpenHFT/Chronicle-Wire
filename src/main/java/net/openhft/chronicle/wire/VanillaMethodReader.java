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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.MethodReaderInterceptorReturns;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.util.InvocationTargetRuntimeException;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static net.openhft.chronicle.wire.VanillaWireParser.SKIP_READABLE_BYTES;

/**
 * This is the VanillaMethodReader class implementing the MethodReader interface.
 * The class primarily handles reading methods from a MarshallableIn source and provides related utilities.
 * It works with WireParselet, MethodReaderInterceptorReturns, and other constructs to facilitate the reading process.
 */
@SuppressWarnings({"rawtypes","this-escape"})
public class VanillaMethodReader implements MethodReader {

    // beware enabling DEBUG_ENABLED as logMessage will not work unless Wire marshalling used - https://github.com/ChronicleEnterprise/Chronicle-Services/issues/240
    public static final boolean DEBUG_ENABLED = Jvm.isDebugEnabled(VanillaMethodReader.class) && Jvm.getBoolean("wire.mr.debug");
    static final Object[] NO_ARGS = {};
    static final Object IGNORED = new Object(); // object used to flag that the call should be ignored.
    private final MarshallableIn in;
    @NotNull
    private final WireParser metaWireParser;
    private final WireParser dataWireParser;
    private final MethodReaderInterceptorReturns methodReaderInterceptorReturns;
    private final Predicate<MethodReader> predicate;

    private MessageHistory messageHistory;
    private boolean closeIn = false;
    private boolean closed;

    /**
     * Constructor for creating an instance of VanillaMethodReader with specified parameters.
     * It uses default values for certain parameters like SKIP_READABLE_BYTES and creates the instance accordingly.
     *
     * @param in The MarshallableIn source from which methods are read.
     * @param ignoreDefault Flag to determine if defaults should be ignored.
     * @param defaultParselet The default parselet to use.
     * @param methodReaderInterceptorReturns The interceptor for method reading.
     * @param metaDataHandler Metadata handler array.
     * @param objects Varargs for additional parameters.
     */
    @UsedViaReflection
    public VanillaMethodReader(MarshallableIn in,
                               boolean ignoreDefault,
                               WireParselet defaultParselet,
                               MethodReaderInterceptorReturns methodReaderInterceptorReturns,
                               Object[] metaDataHandler,
                               @NotNull Object... objects) {
        this(in, ignoreDefault, defaultParselet, SKIP_READABLE_BYTES, methodReaderInterceptorReturns, metaDataHandler, objects);
    }

    @UsedViaReflection
    public VanillaMethodReader(MarshallableIn in,
                               boolean ignoreDefault,
                               WireParselet defaultParselet,
                               FieldNumberParselet fieldNumberParselet,
                               MethodReaderInterceptorReturns methodReaderInterceptorReturns,
                               @NotNull Object... objects) {
        this(in, ignoreDefault, defaultParselet, fieldNumberParselet, methodReaderInterceptorReturns, null, objects);
    }

    /**
     * This constructor is an overloaded version of the primary VanillaMethodReader constructor.
     * It initializes the reader with default predicate logic (always true) and delegates the initialization
     * to the primary constructor.
     *
     * @param in The MarshallableIn source from which methods are read.
     * @param ignoreDefault Flag to determine if defaults should be ignored.
     * @param defaultParselet The default parselet to be used for parsing.
     * @param fieldNumberParselet Custom parselet for handling field numbers.
     * @param methodReaderInterceptorReturns Interceptor for reading methods.
     * @param metaDataHandler Metadata handler array.
     * @param objects Varargs for additional parameters, including custom parselets.
     */
    public VanillaMethodReader(MarshallableIn in,
                               boolean ignoreDefault,
                               WireParselet defaultParselet,
                               FieldNumberParselet fieldNumberParselet,
                               MethodReaderInterceptorReturns methodReaderInterceptorReturns,
                               Object[] metaDataHandler,
                               @NotNull Object... objects) {
        this(in,
                ignoreDefault,
                defaultParselet,
                fieldNumberParselet,
                methodReaderInterceptorReturns, metaDataHandler,
                x -> true, objects);
    }

    /**
     * This is the primary constructor for the VanillaMethodReader class. It initializes the reader
     * with a provided input source, parsing strategies, interceptors, metadata handlers, and other configurations.
     * It supports a comprehensive configuration by accepting multiple parameters and sets up parsers accordingly.
     *
     * @param in The MarshallableIn source from which methods are read.
     * @param ignoreDefault Flag to determine if defaults should be ignored.
     * @param defaultParselet The default parselet to be used for parsing.
     * @param fieldNumberParselet Custom parselet for handling field numbers.
     * @param methodReaderInterceptorReturns Interceptor for reading methods.
     * @param metaDataHandler Metadata handler array.
     * @param predicate Predicate for filtering.
     * @param objects Varargs for additional parameters, including custom parselets.
     */
    public VanillaMethodReader(MarshallableIn in,
                               boolean ignoreDefault,
                               WireParselet defaultParselet,
                               FieldNumberParselet fieldNumberParselet,
                               MethodReaderInterceptorReturns methodReaderInterceptorReturns,
                               Object[] metaDataHandler,
                               Predicate<MethodReader> predicate,
                               @NotNull Object... objects) {
        this.in = in;
        this.methodReaderInterceptorReturns = methodReaderInterceptorReturns;
        this.predicate = predicate;

        // If the first object in the varargs is of type WireParselet, set it as the default parselet
        if (objects[0] instanceof WireParselet)
            defaultParselet = (WireParselet) objects[0];

        // Set up wire parsers with default actions and strategies
        metaWireParser = WireParser.wireParser((s, in0) -> in0.skipValue());
        dataWireParser = WireParser.wireParser(defaultParselet, fieldNumberParselet);

        // Add parsers for components based on provided configurations and objects
        addParsersForComponents(metaWireParser, ignoreDefault,
                addObjectsToMetaDataHandlers(metaDataHandler, objects));
        addParsersForComponents(dataWireParser, ignoreDefault, objects);

        // Add a parser for the message history if not already present
        if (dataWireParser.lookup(HISTORY) == null) {
            dataWireParser.register(new MethodWireKey(HISTORY, MESSAGE_HISTORY_METHOD_ID), (s, v) -> v.marshallable(messageHistory));
        }
    }

    /**
     * Fetches the LongConversion annotation for the first parameter of the provided method.
     * If no such annotation exists, it returns null.
     *
     * @param m The method whose parameter annotations are to be checked.
     * @return The LongConversion annotation for the first parameter or null if not present.
     */
    private static LongConversion longConversionForFirstParam(Method m) {
        Annotation[][] annotations = m.getParameterAnnotations();
        // Check if there are any annotations for the first parameter
        if (annotations.length < 1 || annotations[0].length < 1)
            return null;
        // Loop through all annotations of the first parameter
        for (Annotation annotation : annotations[0]) {
            if (annotation instanceof LongConversion)
                return (LongConversion) annotation;
        }
        return null;
    }

    /**
     * Invokes the provided method with a single long argument, which is parsed or converted based on certain conditions.
     * Provides support for debug logging, context updates, method interception, and exception handling.
     *
     * @param o The object on which the method is to be invoked.
     * @param context The context of the method invocation.
     * @param m The method to be invoked.
     * @param name Name of the method (currently unused but might be useful for future logging or debugging).
     * @param mh Optional MethodHandle for invoking the method (may be null).
     * @param argArr Arguments array for the method.
     * @param s The CharSequence input (used for logging in debug mode).
     * @param v ValueIn from which the long argument is extracted.
     * @param methodReaderInterceptor Interceptor for the method invocation.
     */
    private static void invokeMethodWithOneLong(Object o, Object[] context, @NotNull Method m, String name, MethodHandle mh, Object[] argArr, CharSequence s, ValueIn v, MethodReaderInterceptorReturns methodReaderInterceptor) {
        try {
            // Log the message if debugging is enabled
            if (Jvm.isDebug())
                logMessage(s, v);

            // Update the context if it's null
            if (context[0] == null)
                updateContext(context, o);

            // Parse or convert the argument from the input ValueIn
            long arg = 0;
            if (v.isBinary()) {
                arg = v.int64();
            } else {
                LongConversion lc = longConversionForFirstParam(m);
                if (lc == null) {
                    arg = v.int64();
                } else {
                    String text = v.text();
                    if (text != null && !text.isEmpty()) {
                        LongConverter longConverter = (LongConverter) ObjectUtils.newInstance(lc.value());
                        arg = longConverter.parse(text);
                    }
                }
            }

            // Handle method interception
            if (methodReaderInterceptor != null) {
                argArr[0] = arg;
                Object intercept = methodReaderInterceptor.intercept(m, context[0], argArr, VanillaMethodReader::actualInvoke);
                updateContext(context, intercept);
            } else {
                if (mh == null) {
                    argArr[0] = arg;
                    updateContext(context, m.invoke(context[0], argArr));
                } else {
                    try {
                        if (m.getReturnType() == void.class) {
                            mh.invokeExact(arg);
                            updateContext(context, null);
                        } else {
                            updateContext(context, mh.invokeExact(arg));
                        }
                    } catch (Throwable t) {
                        throw new InvocationTargetException(t);
                    }
                }
            }
        } catch (InvocationTargetException e) {
            throw new InvocationTargetRuntimeException(e);
        } catch (Throwable e) {
            String msg = "Failure to dispatch message: " + m.getName() + " " + Arrays.asList(argArr);
            Jvm.warn().on(o.getClass(), msg, e);
        }
    }

    /**
     * Updates the provided context with the intercepted object.
     *
     * @param context The array containing the context that will be updated.
     * @param intercept The intercepted object that will be set as the new context.
     */
    private static void updateContext(Object[] context, Object intercept) {
//        System.err.println("context: " + (intercept == null ? null : intercept.getClass()));
        context[0] = intercept;
    }

    /**
     * Invokes the specified method on the provided object with the given arguments.
     *
     * @param method The method to be invoked.
     * @param o The object on which the method is to be invoked.
     * @param objects The arguments for the method.
     * @return The result of the method invocation.
     * @throws InvocationTargetException if the method invocation fails.
     */
    private static Object actualInvoke(Method method, Object o, Object[] objects) throws InvocationTargetException {
        try {
            return method.invoke(o, objects);
        } catch (IllegalAccessException iae) {
            throw Jvm.rethrow(iae);
        }
    }

    /**
     * Logs a message if the debug mode is enabled.
     *
     * @param s A CharSequence representing part of the message.
     * @param v The ValueIn associated with the message.
     */
    public static void logMessage(@NotNull CharSequence s, @NotNull ValueIn v) {
        if (!DEBUG_ENABLED) {
            return;
        }

        Jvm.debug().on(VanillaMethodReader.class, logMessage0(s, v));
    }

    /**
     * Constructs a log message based on the provided CharSequence and ValueIn.
     * Converts binary messages to text for better logging.
     *
     * @param s A CharSequence representing part of the message.
     * @param v The ValueIn associated with the message.
     * @return A constructed log message.
     */
    // package local for testing
    static @NotNull String logMessage0(@NotNull CharSequence s, @NotNull ValueIn v) {
        try {
            String rest;

            // Convert binary to text for logging, or retrieve the string representation
            if (v.wireIn().isBinary()) {
                final Bytes<?> bytes0 = v.wireIn().bytes();
                Bytes<?> bytes = Bytes.allocateElasticOnHeap((int) (bytes0.readRemaining() * 3 / 2 + 64));
                long pos = bytes0.readPosition();
                try {
                    v.wireIn().copyTo(WireType.TEXT.apply(bytes));
                    rest = bytes.toString();
                } catch (Exception t) {
                    rest = bytes0.toHexString(pos, bytes0.readLimit() - pos);
                } finally {
                    bytes0.readPosition(pos);
                    bytes.releaseLast();
                }
            } else {
                rest = v.toString();
            }

            // Remove any newline characters from the end of the text representation
            if (rest.endsWith("\n"))
                rest = rest.substring(0, rest.length() - 1);
            return "read " + s + " - " + rest;
        } catch (Exception e) {
            return "read " + s + " - " + e;
        }
    }

    /**
     * Merges the given metaDataHandler and objects arrays, ensuring no duplicates and
     * maintaining the original order. If the metaDataHandler is null, it returns the objects array.
     *
     * @param metaDataHandler The initial metadata handlers to be merged.
     * @param objects The objects to be added to the metadata handlers.
     * @return A merged array containing unique entries from both arrays.
     */
    private Object[] addObjectsToMetaDataHandlers(Object[] metaDataHandler, @NotNull Object @NotNull [] objects) {
        if (metaDataHandler == null) {
            metaDataHandler = objects;
        } else {
            Set<Object> metaDataHandlerSet = new LinkedHashSet<>();
            Collections.addAll(metaDataHandlerSet, metaDataHandler);
            Collections.addAll(metaDataHandlerSet, objects);
            metaDataHandler = metaDataHandlerSet.toArray();
        }
        return metaDataHandler;
    }

    /**
     * Configures the provided WireParser with parselets based on the provided objects.
     * This ensures that each method signature is only added once and that only one filter
     * on the first argument is supported. Interfaces implemented by each object are examined
     * to define these parselets.
     *
     * @param wireParser The WireParser to be configured.
     * @param ignoreDefault If true, defaults are ignored.
     * @param objects The objects that provide the necessary information for configuring the parser.
     */
    private void addParsersForComponents(WireParser wireParser, boolean ignoreDefault, @NotNull Object @NotNull [] objects) {
        // Sets to keep track of method signatures and names that are already handled.
        @NotNull Set<String> methodsSignaturesHandled = new HashSet<>();
        @NotNull Set<String> methodsNamesHandled = new HashSet<>();
        MethodFilterOnFirstArg methodFilterOnFirstArg = null;
        for (@NotNull Object o : objects) {
            if (o instanceof MethodFilterOnFirstArg) {
                if (methodFilterOnFirstArg != null)
                    Jvm.warn().on(getClass(), "Multiple filters on first arg not supported, only the first one is applied.");
                else
                    methodFilterOnFirstArg = (MethodFilterOnFirstArg) o;
            }
            Class<?> oClass = o.getClass();
            Object[] context = {null};
            Supplier<Object> original = () -> o;
            Supplier<Object> inarray = () -> context[0];
            Set<Class> interfaces = new LinkedHashSet<>();

            // Loop through interfaces of the object's class and configure the parser.
            for (Class<?> anInterface : ReflectionUtil.interfaces(oClass)) {
                addParsletsFor(wireParser, interfaces, anInterface, ignoreDefault, methodsNamesHandled, methodsSignaturesHandled, methodFilterOnFirstArg, o, context, original, inarray);
            }
        }
    }

    /**
     * Configures the provided WireParser with parselets based on the methods found in the given class.
     * This method is recursive and also evaluates interfaces that the given class might extend or implement.
     * The main focus is to ensure each method signature is only added once, and to properly handle methods
     * with varying numbers of arguments.
     *
     * @param wireParser           The WireParser to be configured.
     * @param interfaces           A set of interfaces that have already been processed. Used to avoid cyclic processing.
     * @param oClass               The class or interface to evaluate for methods.
     * @param ignoreDefault        If true, methods from default interfaces are ignored.
     * @param methodNamesHandled   A set of method names that have already been handled.
     * @param methodsSignaturesHandled A set of method signatures that have already been handled.
     * @param methodFilterOnFirstArg   Optional filter that can be applied on methods based on their first argument.
     * @param o                    The original object that the method might be invoked on.
     * @param context              The context in which the method will be invoked.
     * @param contextSupplier      Provides the current context.
     * @param nextContext          Provides the next context in which the method will be invoked.
     */
    private void addParsletsFor(WireParser wireParser, Set<Class> interfaces, Class<?> oClass, boolean ignoreDefault, Set<String> methodNamesHandled, Set<String> methodsSignaturesHandled, MethodFilterOnFirstArg methodFilterOnFirstArg, Object o, Object[] context, Supplier contextSupplier, Supplier nextContext) {
        if (!oClass.isInterface() || Jvm.dontChain(oClass)) {
            return;
        }
        if (!interfaces.add(oClass))
            return;

        // Evaluate each method of the class or interface.
        for (@NotNull Method m : oClass.getMethods()) {
            // Basic filtering of methods that should not be processed.
            Class<?> declaringClass = m.getDeclaringClass();
            if (declaringClass == Object.class)
                continue;
            if (Modifier.isStatic(m.getModifiers()))
                continue;
            if (ignoreDefault && declaringClass.isInterface())
                continue;
            if ("ignoreMethodBasedOnFirstArg".equals(m.getName()))
                continue;
            if (!methodsSignaturesHandled.add(signature(m)))
                continue;

            // Ensure the method isn't one from the Object class.
            try {
                // skip Object defined methods.
                Object.class.getMethod(m.getName(), m.getParameterTypes());
                continue;
            } catch (NoSuchMethodException e) {
                // not an Object method.
            }

            if (!methodNamesHandled.add(m.getName())) {
                String previous = methodsSignaturesHandled.stream().filter(signature -> signature.contains(" " + m.getName() + " ")).findFirst().orElseThrow(IllegalStateException::new);
                String msg = m + " previous: " + previous;
                throw new IllegalStateException("MethodReader does not support overloaded methods. Method: " + msg);
            }

            Class<?>[] parameterTypes = m.getParameterTypes();
            // Add the method to the WireParser based on its number of parameters.
            switch (parameterTypes.length) {
                case 0:
                    addParseletForMethod(wireParser, o, context, contextSupplier, m);
                    break;
                case 1:
                    addParseletForMethod(wireParser, o, context, contextSupplier, m, parameterTypes[0]);
                    break;
                default:
                    if (methodFilterOnFirstArg == null)
                        addParseletForMethod(wireParser, o, context, contextSupplier, m, parameterTypes);
                    else
                        addParseletForMethod(wireParser, o, context, contextSupplier, m, parameterTypes, methodFilterOnFirstArg);
                    break;
            }
        }

        // Recursive step: also process interfaces that the current class or interface extends or implements.
        for (@NotNull Method m : oClass.getMethods()) {
            Class<?> returnType = m.getReturnType();
            addParsletsFor(wireParser, interfaces, returnType, ignoreDefault, methodNamesHandled, methodsSignaturesHandled, methodFilterOnFirstArg, o, context, nextContext, nextContext);
        }
    }

    /**
     * Returns a unique signature for the given method, encapsulating its return type, name, and parameter types.
     * The signature is helpful in differentiating between overloaded methods and for debugging purposes.
     *
     * @param m The method for which the signature needs to be generated
     * @return The signature in the format: return_type method_name [param_types]
     */
    private String signature(Method m) {
        return m.getReturnType() + " " + m.getName() + " " + Arrays.toString(m.getParameterTypes());
    }

    /**
     * Sets the closeIn state of the VanillaMethodReader. When closeIn is true, the reader will be
     * automatically closed after reading, otherwise, it remains open for further operations.
     *
     * @param closeIn The new closeIn state to be set
     * @return The current instance of the VanillaMethodReader for chaining method calls
     * @throws IllegalStateException if the VanillaMethodReader is already closed
     */
    @NotNull
    public VanillaMethodReader closeIn(boolean closeIn) {
        throwExceptionIfClosed();

        this.closeIn = closeIn;
        return this;
    }

    /**
     * Checks if the VanillaMethodReader is closed.
     *
     * @throws IllegalStateException if the VanillaMethodReader is closed
     */
    public void throwExceptionIfClosed() {
        if (isClosed())
            throw new IllegalStateException("Closed");
    }

    /**
     * Registers a method with the provided WireParser, enabling parsing of wire messages to trigger method invocations.
     * The method is made accessible, and various checks and optimizations are performed based on the parameter type
     * of the method to determine the most efficient way to handle parsing and invocation.
     *
     * @param wireParser      The WireParser to which the method will be registered
     * @param o2              The object on which the method should be invoked
     * @param context         The current context for method invocation
     * @param contextSupplier Supplies the context for method invocation
     * @param m               The method to be registered
     * @param parameterType   The parameter type of the method being registered
     * @throws IllegalStateException if the VanillaMethodReader is closed
     */
    public void addParseletForMethod(WireParser wireParser, Object o2, Object[] context, Supplier contextSupplier, @NotNull Method m, Class<?> parameterType) {
        throwExceptionIfClosed();

        // Make the method accessible to bypass security checks for faster invocations
        Jvm.setAccessible(m);
        String name = m.getName();
        Class<?> parameterType2 = ObjectUtils.implementationToUse(parameterType);
        if (parameterType == long.class && o2 != null) {
            try {
                MethodHandle mh = m.getDeclaringClass().isInstance(o2) ? MethodHandles.lookup().unreflect(m).bindTo(o2) : null;
                @NotNull Object[] argArr = {null};
                MethodWireKey key = createWireKey(m, name);
                wireParser.registerOnce(key, (s, v) -> invokeMethodWithOneLong(o2, context, m, name, mh, argArr, s, v, methodReaderInterceptorReturns));
            } catch (IllegalAccessException e) {
                Jvm.warn().on(o2.getClass(), "Unable to unreflect " + m, e);
            }
        } else if (parameterType.isPrimitive() || parameterType2.isInterface() || !ReadMarshallable.class.isAssignableFrom(parameterType2)) {
            @NotNull Object[] argArr = {null};
            MethodWireKey key = createWireKey(m, name);
            wireParser.registerOnce(key, (s, v) -> {
                if (Jvm.isDebug())
                    logMessage(s, v);

                argArr[0] = v.object(checkRecycle(argArr[0]), parameterType2);
                if (context[0] == null)
                    updateContext(context, o2);
                Object invoke = invoke(context[0], m, argArr);
                updateContext(context, invoke);
            });

        } else {
            ReadMarshallable arg = (ReadMarshallable) ObjectUtils.newInstance(parameterType2);
            @NotNull ReadMarshallable[] argArr = {arg};
            MethodWireKey key = createWireKey(m, name);
            wireParser.registerOnce(key, (s, v) -> {
                if (Jvm.isDebug())
                    logMessage(s, v);

                //noinspection ConstantConditions
                argArr[0] = v.object(checkRecycle(argArr[0]), Jvm.uncheckedCast(parameterType2));
                if (context[0] == null)
                    updateContext(context, o2);
                Object invoke = invoke(contextSupplier.get(), m, argArr);
                updateContext(context, invoke);
            });
        }
    }

    /**
     * Registers a method with no arguments with the provided WireParser.
     * When a wire message is parsed that matches this method, the method will be invoked without arguments.
     *
     * @param wireParser      The WireParser to which the method will be registered
     * @param o2              The object on which the method should be invoked
     * @param context         The current context for method invocation
     * @param contextSupplier Supplies the context for method invocation
     * @param m               The method to be registered
     * @throws IllegalStateException if the VanillaMethodReader is closed
     */
    public void addParseletForMethod(WireParser wireParser, Object o2, Object[] context, Supplier contextSupplier, @NotNull Method m) {
        throwExceptionIfClosed();

        Jvm.setAccessible(m); // turn of security check to make a little faster
        String name = m.getName();
        MethodWireKey key = createWireKey(m, name);
        wireParser.registerOnce(key, (s, v) -> {
            if (Jvm.isDebug())
                logMessage(s, v);

            v.skipValue();

            Object invoke = invoke(contextSupplier.get(), m, NO_ARGS);
            if (invoke != null)
                updateContext(context, invoke);
            else if (o2 != null)
                updateContext(context, o2);
        });
    }

    /**
     * Creates a MethodWireKey for a given method. This key is used for method registration and matching during parsing.
     * If the method has a MethodId annotation, the key will be based on the annotation's value, otherwise, it will be based
     * on the method's name's hash code.
     *
     * @param m    The method for which the key is being generated
     * @param name The name of the method
     * @return A MethodWireKey uniquely representing the method
     */
    @NotNull
    protected MethodWireKey createWireKey(@NotNull Method m, String name) {
        MethodId annotation = Jvm.findAnnotation(m, MethodId.class);
        return new MethodWireKey(name, annotation == null
                ? name.hashCode()
                : Maths.toInt32(annotation.value()));
    }

    /**
     * Registers a method with multiple arguments with the provided WireParser.
     * The method's arguments are read from the wire message using a sequence reader, which ensures
     * each argument is correctly parsed and matched to its corresponding parameter type.
     *
     * @param wireParser      The WireParser to which the method will be registered
     * @param o2              The object on which the method should be invoked
     * @param context         The current context for method invocation
     * @param contextSupplier Supplies the context for method invocation
     * @param m               The method to be registered
     * @param parameterTypes  The array of parameter types for the method
     * @throws IllegalStateException if the VanillaMethodReader is closed
     */
    public void addParseletForMethod(WireParser wireParser, Object o2, Object[] context, Supplier contextSupplier, @NotNull Method m, @NotNull Class[] parameterTypes) {
        throwExceptionIfClosed();

        Jvm.setAccessible(m); // turn of security check to make a little faster
        @NotNull Object[] args = new Object[parameterTypes.length];
        @NotNull BiConsumer<Object[], ValueIn> sequenceReader = (a, v) -> {
            int i = 0;
            for (@NotNull Class<?> clazz : parameterTypes) {
                a[i] = v.object(checkRecycle(a[i]), clazz);
                i++;
            }
        };
        String name = m.getName();
        MethodWireKey key = createWireKey(m, name);
        wireParser.registerOnce(key, (s, v) -> {
            if (Jvm.isDebug())
                logMessage(s, v);

            v.sequence(args, sequenceReader);

            Object invoke = invoke(contextSupplier.get(), m, args);
            if (invoke != null)
                updateContext(context, invoke);
            else if (o2 != null)
                updateContext(context, o2);
        });
    }

    /**
     * Checks if the given object can be recycled. For collections, it clears the collection and returns the same instance.
     * If the object is an instance of Marshallable, it returns the same object; otherwise, it returns null.
     *
     * @param <T> The type of the object
     * @param o   The object to check
     * @return The original object if it can be recycled, otherwise null
     */
    private <T> T checkRecycle(T o) {
        if (o instanceof Collection<?>) {
            ((Collection<?>) o).clear();
            return o;
        }
        if (o instanceof Map) {
            ((Map<?, ?>) o).clear();
            return o;
        }
        // For objects of type AbstractMarshallableCfg, reset them to their default state.
        if (o instanceof AbstractMarshallableCfg) {
            ((AbstractMarshallableCfg) o).reset();
        }
        return o instanceof Marshallable ? o : null;
    }

    /**
     * Registers a method with multiple arguments with the provided WireParser, applying a filter on the first argument.
     * If the filter determines that the method should be ignored based on its first argument,
     * the method will not be executed and subsequent arguments will be skipped.
     *
     * @param wireParser              The WireParser to which the method will be registered
     * @param o2                      The object on which the method should be invoked
     * @param context                 The current context for method invocation
     * @param contextSupplier         Supplies the context for method invocation
     * @param m                       The method to be registered
     * @param parameterTypes          The array of parameter types for the method
     * @param methodFilterOnFirstArg  The filter that decides if the method should be ignored based on its first argument
     * @throws IllegalStateException  If the VanillaMethodReader is closed
     */
    @SuppressWarnings("unchecked")
    public void addParseletForMethod(WireParser wireParser, Object o2, Object[] context, Supplier contextSupplier, @NotNull Method m, @NotNull Class[] parameterTypes, MethodFilterOnFirstArg methodFilterOnFirstArg) {
        // Ensure the reader is not closed
        throwExceptionIfClosed();

        Jvm.setAccessible(m);

        // Create an array to store the arguments
        @NotNull Object[] args = new Object[parameterTypes.length];

        // Define a sequence reader that fills the args array and applies the method filter
        @NotNull BiConsumer<Object[], ValueIn> sequenceReader = (a, v) -> {
            int i = 0;
            boolean ignored = false;
            for (@NotNull Class<?> clazz : parameterTypes) {
                if (ignored) {
                    // Skip reading the value if previously ignored
                    v.skipValue();
                } else {
                    // Read the object and check if it can be recycled
                    a[i] = v.object(checkRecycle(a[i]), clazz);
                }
                if (i == 0 && methodFilterOnFirstArg.ignoreMethodBasedOnFirstArg(m.getName(), a[0])) {
                    // If the first argument causes the method to be ignored, set the flag
                    a[0] = IGNORED;
                    ignored = true;
                }
                i++;
            }
        };

        // Get the method name
        String name = m.getName();

        // Create a unique key for the method
        MethodWireKey key = createWireKey(m, name);

        // Register the method with the WireParser
        wireParser.registerOnce(key, (s, v) -> {
            if (Jvm.isDebug())
                logMessage(s, v);

            // Fill the args array using the sequence reader
            v.sequence(args, sequenceReader);

            // Exit early if the first argument indicates the method should be ignored
            if (args[0] == IGNORED) {
                args[0] = null;
                return;
            }

            // Invoke the method and update the context if needed
            Object invoke = invoke(contextSupplier.get(), m, args);
            if (invoke != null)
                updateContext(context, invoke);
            else if (o2 != null)
                updateContext(context, o2);
        });
    }

    /**
     * Invokes a method on an object with the provided arguments. If an interceptor is provided,
     * it will use the interceptor to invoke the method.
     * If any exceptions are encountered during the invocation, appropriate warnings or exceptions are raised.
     *
     * @param o     The object on which to invoke the method
     * @param m     The method to invoke
     * @param args  The arguments to pass to the method
     * @return      The result of the method invocation
     * @throws InvocationTargetRuntimeException if the invoked method itself throws an exception
     */
    protected Object invoke(Object o, @NotNull Method m, Object[] args) throws InvocationTargetRuntimeException {
        try {
            // If an interceptor is provided, use it to invoke the method
            if (methodReaderInterceptorReturns != null)
                return methodReaderInterceptorReturns.intercept(m, o, args, VanillaMethodReader::actualInvoke);
            else
                // Otherwise, directly invoke the method
                return m.invoke(o, args);
        } catch (InvocationTargetException e) {
            // If the invoked method throws an exception, wrap it in a custom runtime exception
            throw new InvocationTargetRuntimeException(e.getCause());
        } catch (IllegalAccessException e) {
            // If we don't have access to the method, log a warning
            Throwable cause = e.getCause();
            String msg = "Failure to dispatch message: " + m.getName() + " " + Arrays.asList(args);
            if (cause instanceof IllegalArgumentException)
                Jvm.warn().on(o.getClass(), msg + " " + cause);
            else
                Jvm.warn().on(o.getClass(), msg, cause);
            return null;
        }
    }

    /**
     * Reads a single message. If the message is metadata, it returns true even if it's ignored.
     *
     * @return true if a message was read or if metadata was ignored; false if no more data is available
     * @throws InvocationTargetRuntimeException if an exception occurs during message reading
     */
    public boolean readOne() throws InvocationTargetRuntimeException {
        // Ensure that the reader isn't closed
        throwExceptionIfClosed();
        // return readOne0();

        // Apply the predicate to the current object
        boolean test = predicate.test(this);

        // Return true if the predicate was true and a message was read
        return test && readOne0();
    }

    /**
     * Internal method to read a single message. This handles reading metadata messages
     * and data messages, and also manages the message history.
     *
     * @return true if a message was read; false otherwise
     */
    private boolean readOne0() {
        try (DocumentContext context = in.readingDocument()) {

            // If the document context isn't present, return false indicating no message was read
            if (!context.isPresent()) {
                return false;
            }

            // If the document context is metadata, parse it and return true
            if (context.isMetaData()) {
                metaWireParser.accept(context.wire());
                return true;
            }
            assert context.isData();

            // Reset the message history with the current context's source ID and index
            messageHistory().reset(context.sourceId(), context.index());

            // Parse the data message
            dataWireParser.accept(context.wire());
        } finally {
            messageHistory().reset();
        }
        return true;
    }

    /**
     * Retrieves the current message history instance. If it doesn't exist,
     * it initializes and retrieves the global message history.
     *
     * @return The current message history instance
     */
    private MessageHistory messageHistory() {
        if (messageHistory == null) messageHistory = MessageHistory.get();
        return messageHistory;
    }

    @Override
    public void close() {
        if (closeIn)
            Closeable.closeQuietly(in);
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    /**
     * Retrieves the current method reader interceptor.
     * The interceptor can be used to alter the behavior of method invocation.
     *
     * @return The current {@link MethodReaderInterceptorReturns} instance.
     * @throws IllegalStateException if the method reader is closed.
     */
    public MethodReaderInterceptorReturns methodReaderInterceptorReturns() {
        throwExceptionIfClosed();

        return methodReaderInterceptorReturns;
    }
}
