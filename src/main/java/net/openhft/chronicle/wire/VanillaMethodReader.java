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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.MethodReaderInterceptorReturns;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.util.Annotations;
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
import java.util.function.Supplier;

import static net.openhft.chronicle.wire.VanillaWireParser.SKIP_READABLE_BYTES;

@SuppressWarnings("rawtypes")
public class VanillaMethodReader implements MethodReader {

    static final Object[] NO_ARGS = {};
    static final Object IGNORED = new Object(); // object used to flag that the call should be ignored.
    @Deprecated(/* to be removed in x.22 */)
    private static final boolean DONT_THROW_ON_OVERLOAD = Jvm.getBoolean("chronicle.mr_overload_dont_throw");
    private static final String[] metaIgnoreList = {"header", "index", "index2index", "roll"};
    // beware enabling DEBUG_ENABLED as logMessage will not work unless Wire marshalling used - https://github.com/ChronicleEnterprise/Chronicle-Services/issues/240
    public static final boolean DEBUG_ENABLED = Jvm.isDebugEnabled(VanillaMethodReader.class) && Jvm.getBoolean("wire.mr.debug");
    private final MarshallableIn in;
    @NotNull
    private final WireParser wireParser;
    private MessageHistory messageHistory;
    private boolean closeIn = false, closed;
    private MethodReaderInterceptorReturns methodReaderInterceptorReturns;

    public VanillaMethodReader(MarshallableIn in,
                               boolean ignoreDefault,
                               WireParselet defaultParselet,
                               MethodReaderInterceptorReturns methodReaderInterceptorReturns,
                               @NotNull Object... objects) {
        this(in, ignoreDefault, defaultParselet, SKIP_READABLE_BYTES, methodReaderInterceptorReturns, objects);
    }

    public VanillaMethodReader(MarshallableIn in,
                               boolean ignoreDefault,
                               WireParselet defaultParselet,
                               FieldNumberParselet fieldNumberParselet,
                               MethodReaderInterceptorReturns methodReaderInterceptorReturns,
                               @NotNull Object... objects) {
        this.in = in;
        this.methodReaderInterceptorReturns = methodReaderInterceptorReturns;
        if (objects[0] instanceof WireParselet)
            defaultParselet = (WireParselet) objects[0];

        wireParser = WireParser.wireParser(defaultParselet, fieldNumberParselet);

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
            for (Class<?> anInterface : ReflectionUtil.interfaces(oClass)) {
                addParsletsFor(interfaces, anInterface, ignoreDefault, methodsNamesHandled, methodsSignaturesHandled, methodFilterOnFirstArg, o, context, original, inarray);
            }
        }
        if (wireParser.lookup(HISTORY) == null) {
            wireParser.register(new MethodWireKey(HISTORY, MESSAGE_HISTORY_METHOD_ID), (s, v) -> v.marshallable(messageHistory));
        }
    }

    private static LongConversion longConversionForFirstParam(Method m) {
        Annotation[][] annotations = m.getParameterAnnotations();
        if (annotations == null || annotations.length < 1 || annotations[0].length < 1)
            return null;
        for (Annotation annotation : annotations[0]) {
            if (annotation instanceof LongConversion)
                return (LongConversion) annotation;
        }
        return null;
    }

    private static void invokeMethodWithOneLong(Object o, Object[] context, @NotNull Method m, String name, MethodHandle mh, Object[] argArr, CharSequence s, ValueIn v, MethodReaderInterceptorReturns methodReaderInterceptor) {
        try {
            if (Jvm.isDebug())
                logMessage(s, v);

            if (context[0] == null)
                updateContext(context, o);
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
                        LongConverter longConverter = ObjectUtils.newInstance(lc.value());
                        arg = longConverter.parse(text);
                    }
                }
            }

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

    private static void updateContext(Object[] context, Object intercept) {
//        System.err.println("context: " + (intercept == null ? null : intercept.getClass()));
        context[0] = intercept;
    }

    private static Object actualInvoke(Method method, Object o, Object[] objects) throws InvocationTargetException {
        try {
            return method.invoke(o, objects);
        } catch (IllegalAccessException iae) {
            throw Jvm.rethrow(iae);
        }
    }

    public static void logMessage(@NotNull CharSequence s, @NotNull ValueIn v) {
        if (!DEBUG_ENABLED) {
            return;
        }

        try {
            @NotNull String name = s.toString();
            String rest;

            if (v.wireIn() instanceof BinaryWire) {
                Bytes bytes = Bytes.elasticByteBuffer((int) (v.wireIn().bytes().readRemaining() * 3 / 2 + 64));
                long pos = v.wireIn().bytes().readPosition();
                try {
                    v.wireIn().copyTo(new TextWire(bytes));
                    rest = bytes.toString();
                } finally {
                    v.wireIn().bytes().readPosition(pos);
                    bytes.releaseLast();
                }
            } else {
                rest = v.toString();
            }
            // TextWire.toString has an \n at the end
            if (rest.endsWith("\n"))
                rest = rest.substring(0, rest.length() - 1);
            Jvm.debug().on(VanillaMethodReader.class, "read " + name + " - " + rest);
        } catch (Exception ignore) {
            // todo commented out til the following is fixed  - https://github.com/ChronicleEnterprise/Chronicle-Services/issues/240
            // Jvm.warn().on(VanillaMethodReader.class, "s=" + s, e);
        }
    }

    private void addParsletsFor(Set<Class> interfaces, Class<?> oClass, boolean ignoreDefault, Set<String> methodNamesHandled, Set<String> methodsSignaturesHandled, MethodFilterOnFirstArg methodFilterOnFirstArg, Object o, Object[] context, Supplier contextSupplier, Supplier nextContext) {
        if (!interfaces.add(oClass))
            return;

        for (@NotNull Method m : oClass.getMethods()) {
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

            try {
                // skip Object defined methods.
                Object.class.getMethod(m.getName(), m.getParameterTypes());
                continue;
            } catch (NoSuchMethodException e) {
                // not an Object method.
            }

            if (!methodNamesHandled.add(m.getName())) {
                String previous = methodsSignaturesHandled.stream().filter(signature -> signature.contains(" " + m.getName() + " ")).findFirst().orElseThrow(() -> new IllegalStateException());
                String msg = m + " previous: " + previous;
                if (DONT_THROW_ON_OVERLOAD)
                    Jvm.warn().on(getClass(), "Unable to support overloaded methods, ignoring " + msg);
                else
                    throw new IllegalStateException("MethodReader does not support overloaded methods. Method: " + msg);
                continue;
            }

            Class<?>[] parameterTypes = m.getParameterTypes();
            switch (parameterTypes.length) {
                case 0:
                    addParseletForMethod(o, context, contextSupplier, m);
                    break;
                case 1:
                    addParseletForMethod(o, context, contextSupplier, m, parameterTypes[0]);
                    break;
                default:
                    if (methodFilterOnFirstArg == null)
                        addParseletForMethod(o, context, contextSupplier, m, parameterTypes);
                    else
                        addParseletForMethod(o, context, contextSupplier, m, parameterTypes, methodFilterOnFirstArg);
                    break;
            }
        }

        // add chained interfaces last.
        for (@NotNull Method m : oClass.getMethods()) {
            Class returnType = m.getReturnType();
            if (returnType.isInterface() && !Jvm.dontChain(returnType)) {
                addParsletsFor(interfaces, returnType, ignoreDefault, methodNamesHandled, methodsSignaturesHandled, methodFilterOnFirstArg, o, context, nextContext, nextContext);
            }
        }
    }

    private String signature(Method m) {
        return m.getReturnType() + " " + m.getName() + " " + Arrays.toString(m.getParameterTypes());
    }

    @NotNull
    public VanillaMethodReader closeIn(boolean closeIn) {
        throwExceptionIfClosed();

        this.closeIn = closeIn;
        return this;
    }

    public void throwExceptionIfClosed() {
        if (isClosed())
            throw new IllegalStateException("Closed");
    }

    // one arg
    public void addParseletForMethod(Object o2, Object[] context, Supplier contextSupplier, @NotNull Method m, Class<?> parameterType) {
        throwExceptionIfClosed();

        Jvm.setAccessible(m); // turn of security check to make a little faster
        String name = m.getName();
        Class parameterType2 = ObjectUtils.implementationToUse(parameterType);
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
                argArr[0] = v.object(checkRecycle(argArr[0]), parameterType2);
                if (context[0] == null)
                    updateContext(context, o2);
                Object invoke = invoke(contextSupplier.get(), m, argArr);
                updateContext(context, invoke);
            });
        }
    }

    private Class<?> contextClass(Supplier contextSupplier) {
        Object o = contextSupplier.get();
        return o == null ? VanillaMethodReader.class : o.getClass();
    }

    // no args
    public void addParseletForMethod(Object o2, Object[] context, Supplier contextSupplier, @NotNull Method m) {
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

    @NotNull
    protected MethodWireKey createWireKey(@NotNull Method m, String name) {
        MethodId annotation = Annotations.getAnnotation(m, MethodId.class);
        return new MethodWireKey(name, annotation == null
                ? name.hashCode()
                : Maths.toInt32(annotation.value()));
    }

    public void addParseletForMethod(Object o2, Object[] context, Supplier contextSupplier, @NotNull Method m, @NotNull Class[] parameterTypes) {
        throwExceptionIfClosed();

        Jvm.setAccessible(m); // turn of security check to make a little faster
        @NotNull Object[] args = new Object[parameterTypes.length];
        @NotNull BiConsumer<Object[], ValueIn> sequenceReader = (a, v) -> {
            int i = 0;
            for (@NotNull Class clazz : parameterTypes) {
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

    private <T> T checkRecycle(T o) {
        if (o instanceof Collection<?>) {
            ((Collection<?>) o).clear();
            return o;
        }
        return o instanceof Marshallable ? o : null;
    }

    @SuppressWarnings("unchecked")
    public void addParseletForMethod(Object o2, Object[] context, Supplier contextSupplier, @NotNull Method m, @NotNull Class[] parameterTypes, MethodFilterOnFirstArg methodFilterOnFirstArg) {
        throwExceptionIfClosed();

        Jvm.setAccessible(m); // turn off security check to make a little faster
        @NotNull Object[] args = new Object[parameterTypes.length];
        @NotNull BiConsumer<Object[], ValueIn> sequenceReader = (a, v) -> {
            int i = 0;
            boolean ignored = false;
            for (@NotNull Class clazz : parameterTypes) {
                if (ignored)
                    v.skipValue();
                else
                    a[i] = v.object(checkRecycle(a[i]), clazz);
                if (i == 0) {
                    if (methodFilterOnFirstArg.ignoreMethodBasedOnFirstArg(m.getName(), a[0])) {
                        a[0] = IGNORED;
                        ignored = true;
                    }
                }
                i++;
            }
        };
        String name = m.getName();
        MethodWireKey key = createWireKey(m, name);
        wireParser.registerOnce(key, (s, v) -> {
            if (Jvm.isDebug())
                logMessage(s, v);

            v.sequence(args, sequenceReader);
            if (args[0] == IGNORED) {
                args[0] = null;
                return;
            }
            Object invoke = invoke(contextSupplier.get(), m, args);
            if (invoke != null)
                updateContext(context, invoke);
            else if (o2 != null)
                updateContext(context, o2);
        });
    }

    protected Object invoke(Object o, @NotNull Method m, Object[] args) throws InvocationTargetRuntimeException {
        try {
            if (methodReaderInterceptorReturns != null)
                return methodReaderInterceptorReturns.intercept(m, o, args, VanillaMethodReader::actualInvoke);
            else
                return m.invoke(o, args);
        } catch (InvocationTargetException e) {
            throw new InvocationTargetRuntimeException(e.getCause());
        } catch (IllegalAccessException e) {
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
     * reads one message
     *
     * @return true if there was a message, or false if no more data is available.
     * If we read a metadata message, <code>true</code> is returned even if it was ignored
     */
    public boolean readOne() throws InvocationTargetRuntimeException {
        throwExceptionIfClosed();

        return readOne0();
    }

    /**
     * @deprecated see {@link net.openhft.chronicle.wire.MarshallableIn#peekDocument()}
     */
    @Deprecated(/* remove in x.23 */)
    @Override
    public boolean lazyReadOne() {
        throwExceptionIfClosed();

        if (!in.peekDocument()) {
            return false;
        }

        return readOne0();
    }

    private boolean readOne0() {
        try (DocumentContext context = in.readingDocument()) {
            if (!context.isPresent()) {
                return false;
            }
            if (context.isMetaData()) {
                readOneMetaData(context);
                return true;
            }
            assert context.isData();

            messageHistory().reset(context.sourceId(), context.index());
            wireParser.accept(context.wire());
        } finally {
            messageHistory().reset();
        }
        return true;
    }

    private MessageHistory messageHistory() {
        if (messageHistory == null) messageHistory = MessageHistory.get();
        return messageHistory;
    }

    private boolean readOneMetaData(DocumentContext context) {
        StringBuilder sb = Wires.acquireStringBuilder();

        Wire wire = context.wire();
        Bytes<?> bytes = wire.bytes();
        long r = bytes.readPosition();
        wire.readEventName(sb);

        for (String s : metaIgnoreList) {
            // we wish to ignore our system meta data field
            if (s.contentEquals(sb))
                return false;
        }

        // roll back position to where is was before we read the SB
        bytes.readPosition(r);
        wireParser.accept(wire);
        return true;
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

    public MethodReaderInterceptorReturns methodReaderInterceptorReturns() {
        throwExceptionIfClosed();

        return methodReaderInterceptorReturns;
    }

    @NotNull
    protected WireParser wireParser() {
        return wireParser;
    }
}
