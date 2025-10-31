/*
 * Copyright 2016-2025 chronicle.software
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
 * Deserialises messages from a {@link MarshallableIn} source and dispatches each
 * one as a method call on the supplied handler objects. A {@link WireParser}
 * maps event names or method identifiers to the correct handler method.
 * <p>
 * The reader can operate either via a slower reflection proxy (deprecated) or
 * a generated high-performance implementation, see {@link GenerateMethodReader}.
 * Use {@link VanillaMethodReaderBuilder} to configure instances.
 * <p>
 * Supports interception via {@link MethodReaderInterceptorReturns} and custom
 * parsing through {@link WireParselet} and {@link FieldNumberParselet}.
 */
@SuppressWarnings({"rawtypes","this-escape"})
public class VanillaMethodReader implements MethodReader {

    // beware enabling DEBUG_ENABLED as logMessage will not work unless Wire marshalling used - https://github.com/ChronicleEnterprise/Chronicle-Services/issues/240
    /**
     * Flag to enable debug logging of messages being read. Controlled by the
     * system property {@code wire.mr.debug}. When true, messages are logged via
     * {@link Jvm#debug()} and performance may suffer.
     */
    public static final boolean DEBUG_ENABLED = Jvm.isDebugEnabled(VanillaMethodReader.class) && Jvm.getBoolean("wire.mr.debug");

    // Shared empty {@code Object} array used to invoke methods with no arguments
    static final Object[] NO_ARGS = {};

    // Sentinel used internally with {@link MethodFilterOnFirstArg} to skip a call
    static final Object IGNORED = new Object();

    // The {@link MarshallableIn} source of messages
    private final MarshallableIn in;

    // Parser used for metadata messages
    @NotNull
    private final WireParser metaWireParser;
    // Parser used for data messages
    private final WireParser dataWireParser;
    // Optional interceptor for dispatched method calls
    private final MethodReaderInterceptorReturns methodReaderInterceptorReturns;

    // Predicate controlling whether a message is processed
    private final Predicate<MethodReader> predicate;

    // Lazily initialised history for the current message
    private MessageHistory messageHistory;

    // When true the {@link #in} is closed when this reader is closed
    private boolean closeIn = false;

    // Flag set once {@link #close()} has been called
    private boolean closed;

    /**
     * Convenience constructor used by generated code.
     *
     * @param in       source of wire messages
     * @param ignoreDefault if true default interface methods are skipped
     * @param defaultParselet parselet for unknown events
     * @param methodReaderInterceptorReturns optional interceptor
     * @param metadataHandlers handlers for metadata messages
     * @param handlers handler instances for data messages
     */
    @UsedViaReflection
    public VanillaMethodReader(MarshallableIn in,
                               boolean ignoreDefault,
                               WireParselet defaultParselet,
                               MethodReaderInterceptorReturns methodReaderInterceptorReturns,
                               Object[] metadataHandlers,
                               @NotNull Object... handlers) {
        this(in, ignoreDefault, defaultParselet, SKIP_READABLE_BYTES, methodReaderInterceptorReturns, metadataHandlers, handlers);
    }

    /**
     * Convenience constructor used by generated code.
     *
     * @param in       source of wire messages
     * @param ignoreDefault if true default interface methods are skipped
     * @param defaultParselet parselet for unknown events
     * @param fieldNumberParselet parselet for numeric ids
     * @param methodReaderInterceptorReturns optional interceptor
     * @param handlers handler instances for data messages
     */
    @UsedViaReflection
    public VanillaMethodReader(MarshallableIn in,
                               boolean ignoreDefault,
                               WireParselet defaultParselet,
                               FieldNumberParselet fieldNumberParselet,
                               MethodReaderInterceptorReturns methodReaderInterceptorReturns,
                               @NotNull Object... handlers) {
        this(in, ignoreDefault, defaultParselet, fieldNumberParselet, methodReaderInterceptorReturns, null, handlers);
    }

    /**
     * Constructor that defaults the predicate to always true.
     * It ultimately calls the primary constructor.
     *
     * @param in       source of wire messages
     * @param ignoreDefault if true default interface methods are skipped
     * @param defaultParselet parselet for unknown events
     * @param fieldNumberParselet parselet for numeric ids
     * @param methodReaderInterceptorReturns optional interceptor
     * @param metadataHandlers handlers for metadata messages
     * @param handlers handler instances for data messages
     */
    public VanillaMethodReader(MarshallableIn in,
                               boolean ignoreDefault,
                               WireParselet defaultParselet,
                               FieldNumberParselet fieldNumberParselet,
                               MethodReaderInterceptorReturns methodReaderInterceptorReturns,
                               Object[] metadataHandlers,
                               @NotNull Object... handlers) {
        this(in,
                ignoreDefault,
                defaultParselet,
                fieldNumberParselet,
                methodReaderInterceptorReturns, metadataHandlers,
                x -> true, handlers);
    }

    /**
     * Primary constructor configuring parsers and interceptors.
     *
     * @param in       source of wire messages
     * @param ignoreDefault if true default interface methods are skipped
     * @param defaultParselet parselet for unknown events
     * @param fieldNumberParselet parselet for numeric ids
     * @param methodReaderInterceptorReturns optional interceptor
     * @param metadataHandlers handlers for metadata messages
     * @param predicate predicate controlling readOne execution
     * @param handlers handler instances for data messages
     */
    public VanillaMethodReader(MarshallableIn in,
                               boolean ignoreDefault,
                               WireParselet defaultParselet,
                               FieldNumberParselet fieldNumberParselet,
                               MethodReaderInterceptorReturns methodReaderInterceptorReturns,
                               Object[] metadataHandlers,
                               Predicate<MethodReader> predicate,
                               @NotNull Object... handlers) {
        this.in = in;
        this.methodReaderInterceptorReturns = methodReaderInterceptorReturns;
        this.predicate = predicate;

        // If the first object in the varargs is of type WireParselet, set it as the default parselet
        if (handlers[0] instanceof WireParselet)
            defaultParselet = (WireParselet) handlers[0];

        // Set up wire parsers with default actions and strategies
        metaWireParser = WireParser.wireParser((s, in0) -> in0.skipValue());
        dataWireParser = WireParser.wireParser(defaultParselet, fieldNumberParselet);

        // Add parsers for components based on provided configurations and objects
        addParsersForComponents(metaWireParser, ignoreDefault,
                addObjectsToMetaDataHandlers(metadataHandlers, handlers));
        addParsersForComponents(dataWireParser, ignoreDefault, handlers);

        // Add a parser for the message history if not already present
        if (dataWireParser.lookup(HISTORY) == null) {
            dataWireParser.register(new MethodWireKey(HISTORY, MESSAGE_HISTORY_METHOD_ID), (s, v) -> v.marshallable(messageHistory));
        }
    }

    /**
     * Finds a {@link LongConversion} on the first parameter of {@code method}.
     * Returns {@code null} when no annotation is present.
     */
    private static LongConversion longConversionForFirstParam(Method method) {
        Annotation[][] annotations = method.getParameterAnnotations();
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
     * Invokes {@code method} on {@code target} (or {@code context[0]}) with one long argument.
     * The argument is read from {@code valueIn}, applying any {@link LongConversion}.
     *
     * @param target       handler when {@code context[0]} is null
     * @param contextHolder invocation context array
     * @param method       method to call
     * @param methodName   method name for logging
     * @param methodHandle optional bound MethodHandle
     * @param argHolder    scratch array for the argument
     * @param eventName    event name being read
     * @param valueIn      value reader
     * @param interceptor  optional interceptor
     */
    private static void invokeMethodWithOneLong(Object target, Object[] contextHolder, @NotNull Method method, MethodHandle methodHandle, Object[] argHolder, CharSequence eventName, ValueIn valueIn, MethodReaderInterceptorReturns interceptor) {
        try {
            // Log the message if debugging is enabled
            if (Jvm.isDebug())
                logMessage(eventName, valueIn);

            // Update the context if it's null
            if (contextHolder[0] == null)
                updateContext(contextHolder, target);

            // Parse or convert the argument from the input ValueIn
            long arg = 0;
            if (valueIn.isBinary()) {
                arg = valueIn.int64();
            } else {
                LongConversion lc = longConversionForFirstParam(method);
                if (lc == null) {
                    arg = valueIn.int64();
                } else {
                    String text = valueIn.text();
                    if (text != null && !text.isEmpty()) {
                        LongConverter longConverter = (LongConverter) ObjectUtils.newInstance(lc.value());
                        arg = longConverter.parse(text);
                    }
                }
            }

            // Handle method interception
            if (interceptor != null) {
                argHolder[0] = arg;
                Object intercept = interceptor.intercept(method, contextHolder[0], argHolder, VanillaMethodReader::actualInvoke);
                updateContext(contextHolder, intercept);
            } else {
                if (methodHandle == null) {
                    argHolder[0] = arg;
                    updateContext(contextHolder, method.invoke(contextHolder[0], argHolder));
                } else {
                    try {
                        if (method.getReturnType() == void.class) {
                            methodHandle.invokeExact(arg);
                            updateContext(contextHolder, null);
                        } else {
                            updateContext(contextHolder, methodHandle.invokeExact(arg));
                        }
                    } catch (Throwable t) {
                        throw new InvocationTargetException(t);
                    }
                }
            }
        } catch (InvocationTargetException e) {
            throw new InvocationTargetRuntimeException(e);
        } catch (Throwable e) {
            String msg = "Failure to dispatch message: " + method.getName() + " " + Arrays.asList(argHolder);
            Jvm.warn().on(target.getClass(), msg, e);
        }
    }

    /**
     * Replace {@code contextHolder[0]} with the supplied object.
     * Used to support chained calls where methods return a new handler.
     */
    private static void updateContext(Object[] contextHolder, Object intercept) {
//        System.err.println("context: " + (intercept == null ? null : intercept.getClass()));
        contextHolder[0] = intercept;
    }

    /**
     * Performs the actual reflective call.
     * Used by the interceptor when present.
     *
     * @param method The method to be invoked.
     * @param target The object on which the method is to be invoked.
     * @param args The arguments for the method.
     * @return The result of the method invocation.
     * @throws InvocationTargetException if the method invocation fails.
     */
    private static Object actualInvoke(Method method, Object target, Object[] args) throws InvocationTargetException {
        try {
            return method.invoke(target, args);
        } catch (IllegalAccessException iae) {
            throw Jvm.rethrow(iae);
        }
    }

    /**
     * Write a debug log entry for the incoming event if the debug mode is enabled..
     */
    public static void logMessage(@NotNull CharSequence eventName, @NotNull ValueIn valueIn) {
        if (!DEBUG_ENABLED) {
            return;
        }

        Jvm.debug().on(VanillaMethodReader.class, logMessage0(eventName, valueIn));
    }

    /**
     * Format the debug message, converting binary to text when required.
     */
    // package local for testing
    static @NotNull String logMessage0(@NotNull CharSequence eventName, @NotNull ValueIn valueIn) {
        try {
            String rest;

            // Convert binary to text for logging, or retrieve the string representation
            if (valueIn.wireIn().isBinary()) {
                final Bytes<?> bytes0 = valueIn.wireIn().bytes();
                Bytes<?> bytes = Bytes.allocateElasticOnHeap((int) (bytes0.readRemaining() * 3 / 2 + 64));
                long pos = bytes0.readPosition();
                try {
                    valueIn.wireIn().copyTo(WireType.TEXT.apply(bytes));
                    rest = bytes.toString();
                } catch (Exception t) {
                    rest = bytes0.toHexString(pos, bytes0.readLimit() - pos);
                } finally {
                    bytes0.readPosition(pos);
                    bytes.releaseLast();
                }
            } else {
                rest = valueIn.toString();
            }

            // Remove any newline characters from the end of the text representation
            if (rest.endsWith("\n"))
                rest = rest.substring(0, rest.length() - 1);
            return "read " + eventName + " - " + rest;
        } catch (Exception e) {
            return "read " + eventName + " - " + e;
        }
    }

    /**
     * Combine explicit metadata handlers with the general handler list.
     *
     * @param metadataHandlers handlers dedicated to metadata messages
     * @param handlers general handler objects
     * @return merged array
     */
    private Object[] addObjectsToMetaDataHandlers(Object[] metadataHandlers, @NotNull Object @NotNull [] handlers) {
        if (metadataHandlers == null) {
            metadataHandlers = handlers;
        } else {
            Set<Object> metaHandlerSet = new LinkedHashSet<>();
            Collections.addAll(metaHandlerSet, metadataHandlers);
            Collections.addAll(metaHandlerSet, handlers);
            metadataHandlers = metaHandlerSet.toArray();
        }
        return metadataHandlers;
    }

    /**
     * Configures the provided WireParser with parselets based on the provided objects.
     * This ensures that each method signature is only added once and that only one filter
     * on the first argument is supported. Interfaces implemented by each object are examined
     * to define these parselets.
     *
     * @param wireParser The WireParser to be configured.
     * @param ignoreDefault If true, defaults are ignored.
     * @param handlers The objects that provide the necessary information for configuring the parser.
     */
    private void addParsersForComponents(WireParser wireParser, boolean ignoreDefault, @NotNull Object @NotNull [] handlers) {
        // Sets to keep track of method signatures and names that are already handled.
        @NotNull Set<String> methodsSignaturesHandled = new HashSet<>();
        @NotNull Set<String> methodsNamesHandled = new HashSet<>();
        MethodFilterOnFirstArg methodFilterOnFirstArg = null;
        for (@NotNull Object o : handlers) {
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
     * @param handlerClass               The class or interface to evaluate for methods.
     * @param ignoreDefault        If true, methods from default interfaces are ignored.
     * @param methodNamesHandled   A set of method names that have already been handled.
     * @param methodsSignaturesHandled A set of method signatures that have already been handled.
     * @param methodFilterOnFirstArg   Optional filter that can be applied on methods based on their first argument.
     * @param handler                    The original object that the method might be invoked on.
     * @param contextHolder              The context in which the method will be invoked.
     * @param contextSupplier      Provides the current context.
     * @param nextContextSupplier          Provides the next context in which the method will be invoked.
     */
    private void addParsletsFor(WireParser wireParser, Set<Class> interfaces, Class<?> handlerClass, boolean ignoreDefault, Set<String> methodNamesHandled, Set<String> methodsSignaturesHandled, MethodFilterOnFirstArg methodFilterOnFirstArg, Object handler, Object[] contextHolder, Supplier contextSupplier, Supplier nextContextSupplier) {
        if (!handlerClass.isInterface() || Jvm.dontChain(handlerClass)) {
            return;
        }
        if (!interfaces.add(handlerClass))
            return;

        // Evaluate each method of the class or interface.
        for (@NotNull Method m : handlerClass.getMethods()) {
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
            boolean isObjectMethod = Arrays.stream(Object.class.getMethods())
                    .anyMatch(objectMethod -> objectMethod.getName().equals(m.getName())
                            && Arrays.equals(objectMethod.getParameterTypes(), m.getParameterTypes()));
            if (isObjectMethod)
                continue;

            if (!methodNamesHandled.add(m.getName())) {
                String previous = methodsSignaturesHandled.stream().filter(signature -> signature.contains(" " + m.getName() + " ")).findFirst().orElseThrow(IllegalStateException::new);
                String msg = m + " previous: " + previous;
                throw new IllegalStateException("MethodReader does not support overloaded methods. Method: " + msg);
            }

            Class<?>[] parameterTypes = m.getParameterTypes();
            // Add the method to the WireParser based on its number of parameters.
            switch (parameterTypes.length) {
                case 0:
                    addParseletForMethod(wireParser, handler, contextHolder, contextSupplier, m);
                    break;
                case 1:
                    addParseletForMethod(wireParser, handler, contextHolder, contextSupplier, m, parameterTypes[0]);
                    break;
                default:
                    if (methodFilterOnFirstArg == null)
                        addParseletForMethod(wireParser, handler, contextHolder, contextSupplier, m, parameterTypes);
                    else
                        addParseletForMethod(wireParser, handler, contextHolder, contextSupplier, m, parameterTypes, methodFilterOnFirstArg);
                    break;
            }
        }

        // Recursive step: also process interfaces that the current class or interface extends or implements.
        for (@NotNull Method m : handlerClass.getMethods()) {
            Class<?> returnType = m.getReturnType();
            addParsletsFor(wireParser, interfaces, returnType, ignoreDefault, methodNamesHandled, methodsSignaturesHandled, methodFilterOnFirstArg, handler, contextHolder, nextContextSupplier, nextContextSupplier);
        }
    }

    /**
     * Returns a unique signature for the given method, encapsulating its return type, name, and parameter types.
     * The signature is helpful in differentiating between overloaded methods and for debugging purposes.
     *
     * @param method The method for which the signature needs to be generated
     * @return The signature in the format: return_type method_name [param_types]
     */
    private String signature(Method method) {
        return method.getReturnType() + " " + method.getName() + " " + Arrays.toString(method.getParameterTypes());
    }

    /**
     * Control whether {@link #close()} also closes the input.
     */
    @NotNull
    public VanillaMethodReader closeIn(boolean closeIn) {
        throwExceptionIfClosed();

        this.closeIn = closeIn;
        return this;
    }

    /**
     * Throw {@link IllegalStateException} if this reader has been closed.
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
     * @param target          The object on which the method should be invoked
     * @param contextHolder   The current context for method invocation
     * @param contextSupplier Supplies the context for method invocation
     * @param method          The method to be registered
     * @param parameterType   The parameter type of the method being registered
     * @throws IllegalStateException if the VanillaMethodReader is closed
     */
    public void addParseletForMethod(WireParser wireParser, Object target, Object[] contextHolder, Supplier contextSupplier, @NotNull Method method, Class<?> parameterType) {
        throwExceptionIfClosed();

        // Make the method accessible to bypass security checks for faster invocations
        Jvm.setAccessible(method);
        String name = method.getName();
        Class<?> parameterType2 = ObjectUtils.implementationToUse(parameterType);
        if (parameterType == long.class && target != null) {
            try {
                MethodHandle mh = method.getDeclaringClass().isInstance(target) ? MethodHandles.lookup().unreflect(method).bindTo(target) : null;
                @NotNull Object[] argArr = {null};
                MethodWireKey key = createWireKey(method, name);
                wireParser.registerOnce(key, (s, v) -> invokeMethodWithOneLong(target, contextHolder, method, mh, argArr, s, v, methodReaderInterceptorReturns));
            } catch (IllegalAccessException e) {
                Jvm.warn().on(target.getClass(), "Unable to unreflect " + method, e);
            }
        } else if (parameterType.isPrimitive() || parameterType2.isInterface() || !ReadMarshallable.class.isAssignableFrom(parameterType2)) {
            @NotNull Object[] argArr = {null};
            MethodWireKey key = createWireKey(method, name);
            wireParser.registerOnce(key, (s, v) -> {
                if (Jvm.isDebug())
                    logMessage(s, v);

                argArr[0] = v.object(checkRecycle(argArr[0]), parameterType2);
                if (contextHolder[0] == null)
                    updateContext(contextHolder, target);
                Object invoke = invoke(contextHolder[0], method, argArr);
                updateContext(contextHolder, invoke);
            });

        } else {
            ReadMarshallable arg = (ReadMarshallable) ObjectUtils.newInstance(parameterType2);
            @NotNull ReadMarshallable[] argArr = {arg};
            MethodWireKey key = createWireKey(method, name);
            wireParser.registerOnce(key, (s, v) -> {
                if (Jvm.isDebug())
                    logMessage(s, v);

                //noinspection ConstantConditions
                argArr[0] = v.object(checkRecycle(argArr[0]), Jvm.uncheckedCast(parameterType2));
                if (contextHolder[0] == null)
                    updateContext(contextHolder, target);
                Object invoke = invoke(contextSupplier.get(), method, argArr);
                updateContext(contextHolder, invoke);
            });
        }
    }

    /**
     * Register a zero argument method with {@code wireParser}.
     */
    public void addParseletForMethod(WireParser wireParser, Object target, Object[] contextHolder, Supplier contextSupplier, @NotNull Method method) {
        throwExceptionIfClosed();

        Jvm.setAccessible(method); // turn of security check to make a little faster
        String name = method.getName();
        MethodWireKey key = createWireKey(method, name);
        wireParser.registerOnce(key, (s, v) -> {
            if (Jvm.isDebug())
                logMessage(s, v);

            v.skipValue();

            Object invoke = invoke(contextSupplier.get(), method, NO_ARGS);
            if (invoke != null)
                updateContext(contextHolder, invoke);
            else if (target != null)
                updateContext(contextHolder, target);
        });
    }

    /**
     * Creates a MethodWireKey for a given method. This key is used for method registration and matching during parsing.
     * If the method has a MethodId annotation, the key will be based on the annotation's value, otherwise, it will be based
     * on the method's name's hash code.
     *
     * @param method    The method for which the key is being generated
     * @param name The name of the method
     * @return A MethodWireKey uniquely representing the method
     */
    @NotNull
    protected MethodWireKey createWireKey(@NotNull Method method, String name) {
        MethodId annotation = Jvm.findAnnotation(method, MethodId.class);
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
     * @param target          The object on which the method should be invoked
     * @param contextHolder   The current context for method invocation
     * @param contextSupplier Supplies the context for method invocation
     * @param method          The method to be registered
     * @param parameterTypes  The array of parameter types for the method
     * @throws IllegalStateException if the VanillaMethodReader is closed
     */
    public void addParseletForMethod(WireParser wireParser, Object target, Object[] contextHolder, Supplier contextSupplier, @NotNull Method method, @NotNull Class[] parameterTypes) {
        throwExceptionIfClosed();

        Jvm.setAccessible(method); // turn of security check to make a little faster
        @NotNull Object[] args = new Object[parameterTypes.length];
        @NotNull BiConsumer<Object[], ValueIn> sequenceReader = (a, v) -> {
            int i = 0;
            for (@NotNull Class<?> clazz : parameterTypes) {
                a[i] = v.object(checkRecycle(a[i]), clazz);
                i++;
            }
        };
        String name = method.getName();
        MethodWireKey key = createWireKey(method, name);
        wireParser.registerOnce(key, (s, v) -> {
            if (Jvm.isDebug())
                logMessage(s, v);

            v.sequence(args, sequenceReader);

            Object invoke = invoke(contextSupplier.get(), method, args);
            if (invoke != null)
                updateContext(contextHolder, invoke);
            else if (target != null)
                updateContext(contextHolder, target);
        });
    }

    /**
     * Checks if the given object can be recycled. For collections, it clears the collection and returns the same instance.
     * If the object is an instance of Marshallable, it returns the same object; otherwise, it returns null.
     *
     * @param <T> The type of the object
     * @param instance   The object to check
     * @return The original object if it can be recycled, otherwise null
     */
    private <T> T checkRecycle(T instance) {
        if (instance instanceof Collection<?>) {
            ((Collection<?>) instance).clear();
            return instance;
        }
        if (instance instanceof Map) {
            ((Map<?, ?>) instance).clear();
            return instance;
        }
        // For objects of type AbstractMarshallableCfg, reset them to their default state.
        if (instance instanceof AbstractMarshallableCfg) {
            ((AbstractMarshallableCfg) instance).reset();
        }
        return instance instanceof Marshallable ? instance : null;
    }

    /**
     * Registers a method with multiple arguments with the provided WireParser, applying a filter on the first argument.
     * If the filter determines that the method should be ignored based on its first argument,
     * the method will not be executed and subsequent arguments will be skipped.
     *
     * @param wireParser              The WireParser to which the method will be registered
     * @param target                  The object on which the method should be invoked
     * @param contextHolder           The current context for method invocation
     * @param contextSupplier         Supplies the context for method invocation
     * @param method                  The method to be registered
     * @param parameterTypes          The array of parameter types for the method
     * @param methodFilterOnFirstArg  The filter that decides if the method should be ignored based on its first argument
     * @throws IllegalStateException  If the VanillaMethodReader is closed
     */
    @SuppressWarnings("unchecked")
    public void addParseletForMethod(WireParser wireParser, Object target, Object[] contextHolder, Supplier contextSupplier, @NotNull Method method, @NotNull Class[] parameterTypes, MethodFilterOnFirstArg methodFilterOnFirstArg) {
        // Ensure the reader is not closed
        throwExceptionIfClosed();

        Jvm.setAccessible(method);

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
                if (i == 0 && methodFilterOnFirstArg.ignoreMethodBasedOnFirstArg(method.getName(), a[0])) {
                    // If the first argument causes the method to be ignored, set the flag
                    a[0] = IGNORED;
                    ignored = true;
                }
                i++;
            }
        };

        // Get the method name
        String name = method.getName();

        // Create a unique key for the method
        MethodWireKey key = createWireKey(method, name);

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
            Object invoke = invoke(contextSupplier.get(), method, args);
            if (invoke != null)
                updateContext(contextHolder, invoke);
            else if (target != null)
                updateContext(contextHolder, target);
        });
    }

    /**
     * Invokes a method on an object with the provided arguments. If an interceptor is provided,
     * it will use the interceptor to invoke the method.
     * If any exceptions are encountered during the invocation, appropriate warnings or exceptions are raised.
     *
     * @param target The object on which to invoke the method
     * @param method The method to invoke
     * @param args   The arguments to pass to the method
     * @return       The result of the method invocation
     * @throws InvocationTargetRuntimeException if the invoked method itself throws an exception
     */
    protected Object invoke(Object target, @NotNull Method method, Object[] args) throws InvocationTargetRuntimeException {
        try {
            // If an interceptor is provided, use it to invoke the method
            if (methodReaderInterceptorReturns != null)
                return methodReaderInterceptorReturns.intercept(method, target, args, VanillaMethodReader::actualInvoke);
            else
                // Otherwise, directly invoke the method
                return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            // If the invoked method throws an exception, wrap it in a custom runtime exception
            throw new InvocationTargetRuntimeException(e.getCause());
        } catch (IllegalAccessException e) {
            // If we don't have access to the method, log a warning
            Throwable cause = e.getCause();
            String msg = "Failure to dispatch message: " + method.getName() + " " + Arrays.asList(args);
            if (cause instanceof IllegalArgumentException)
                Jvm.warn().on(target.getClass(), msg + " " + cause);
            else
                Jvm.warn().on(target.getClass(), msg, cause);
            return null;
        }
    }

    /**
     * Read and dispatch a single message.
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
     * Core logic for {@link #readOne()} handling metadata and message history.
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
     * Lazily obtain the {@link MessageHistory} used for this read cycle.
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
     * Return the configured {@link MethodReaderInterceptorReturns} if any.
     */
    public MethodReaderInterceptorReturns methodReaderInterceptorReturns() {
        throwExceptionIfClosed();

        return methodReaderInterceptorReturns;
    }
}
