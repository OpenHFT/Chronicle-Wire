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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.MethodReaderInterceptorReturns;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.util.Annotations;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
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
    static final Logger LOGGER = LoggerFactory.getLogger(VanillaMethodReader.class);
    static final Object IGNORED = new Object(); // object used to flag that the call should be ignored.
    private static final boolean DONT_THROW_ON_OVERLOAD = Jvm.getBoolean("chronicle.mr_overload_dont_throw");
    private static final String[] metaIgnoreList = {"header", "index", "index2index", "roll"};
    private final MarshallableIn in;
    @NotNull
    private final WireParser wireParser;
    private final MessageHistory messageHistory = MessageHistory.get();
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

        @NotNull Set<String> methodsHandled = new HashSet<>();
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
                addParsletsFor(interfaces, anInterface, ignoreDefault, methodsHandled, methodFilterOnFirstArg, o, context, original, inarray);
            }
        }
        if (wireParser.lookup(HISTORY) == null) {
            wireParser.registerOnce(() -> HISTORY, (s, v) -> v.marshallable(messageHistory));
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
            try {
                if (methodReaderInterceptor != null) {
                    argArr[0] = arg;
                    Object intercept = methodReaderInterceptor.intercept(m, o, argArr, VanillaMethodReader::actualInvoke);
                    if (intercept == null) {
                        context[0] = o;
                    } else {
                        context[0] = intercept;
                    }
                } else {
                    mh.invokeExact(arg);
                }
            } catch (Throwable e) {
                Throwable cause = e.getCause();
                String msg = "Failure to dispatch message: " + m.getName() + " " + Arrays.asList(argArr);
                if (cause instanceof IllegalArgumentException)
                    Jvm.warn().on(o.getClass(), msg + " " + cause);
                else
                    Jvm.warn().on(o.getClass(), msg, cause);
            }
        } catch (Exception i) {
            Jvm.warn().on(o.getClass(), "Failure to dispatch message: " + name + " " + argArr[0], i);
        }
    }

    private static Object actualInvoke(Method method, Object o, Object[] objects) throws InvocationTargetException {
        try {
            return method.invoke(o, objects);
        } catch (IllegalAccessException iae) {
            throw Jvm.rethrow(iae);
        }
    }

    protected static void logMessage(@NotNull CharSequence s, @NotNull ValueIn v) {
        if (!LOGGER.isDebugEnabled()) {
            return;
        }

        @NotNull String name = s.toString();
        String rest;

        if (v.wireIn() instanceof BinaryWire) {
            Bytes bytes = Bytes.elasticByteBuffer((int) (v.wireIn().bytes().readRemaining() * 3 / 2 + 64));
            long pos = v.wireIn().bytes().readPosition();
            v.wireIn().copyTo(new TextWire(bytes));
            v.wireIn().bytes().readPosition(pos);
            rest = bytes.toString();
            bytes.releaseLast();

        } else {
            rest = v.toString();
        }
        // TextWire.toString has an \n at the end
        if (rest.endsWith("\n"))
            rest = rest.substring(0, rest.length() - 1);
        LOGGER.debug("read " + name + " - " + rest);
    }

    private void addParsletsFor(Set<Class> interfaces, Class<?> oClass, boolean ignoreDefault, Set<String> methodsHandled, MethodFilterOnFirstArg methodFilterOnFirstArg, Object o, Object[] context, Supplier contextSupplier, Supplier nextContext) {
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

            try {
                // skip Object defined methods.
                Object.class.getMethod(m.getName(), m.getParameterTypes());
                continue;
            } catch (NoSuchMethodException e) {
                // not an Object method.
            }

            if (!methodsHandled.add(m.getName())) {
                if (DONT_THROW_ON_OVERLOAD)
                    Jvm.warn().on(getClass(), "Unable to support overloaded methods, ignoring one of " + m.getName());
                else
                    throw new IllegalStateException("MethodReader does not support overloaded methods. Method name: " + m.getName());
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
                addParsletsFor(interfaces, returnType, ignoreDefault, methodsHandled, methodFilterOnFirstArg, null, context, nextContext, nextContext);
            }
        }
    }

    @NotNull
    public VanillaMethodReader closeIn(boolean closeIn) {
        throwExceptionIfClosed();

        this.closeIn = closeIn;
        return this;
    }

    // one arg
    public void addParseletForMethod(Object o2, Object[] context, Supplier contextSupplier, @NotNull Method m, Class<?> parameterType) {
        throwExceptionIfClosed();

        Jvm.setAccessible(m); // turn of security check to make a little faster
        String name = m.getName();
        if (parameterType == long.class && o2 != null) {
            try {
                MethodHandle mh = MethodHandles.lookup().unreflect(m).bindTo(o2);
                @NotNull Object[] argArr = {null};
                MethodWireKey key = createWireKey(m, name);
                // TODO: check for LongConversion
                wireParser.registerOnce(key, (s, v) -> invokeMethodWithOneLong(o2, context, m, name, mh, argArr, s, v, methodReaderInterceptorReturns));
            } catch (IllegalAccessException e) {
                Jvm.warn().on(o2.getClass(), "Unable to unreflect " + m, e);
            }
        } else if (parameterType.isPrimitive() || parameterType.isInterface() || !ReadMarshallable.class.isAssignableFrom(parameterType)) {
            @NotNull Object[] argArr = {null};
            MethodWireKey key = createWireKey(m, name);
            wireParser.registerOnce(key, (s, v) -> {
                try {
                    if (Jvm.isDebug())
                        logMessage(s, v);

                    argArr[0] = v.object(checkRecycle(argArr[0]), parameterType);
                    Object invoke = invoke(contextSupplier.get(), m, argArr);
                    if (invoke != null)
                        context[0] = invoke;
                    else if (o2 != null)
                        context[0] = o2;
                } catch (Exception i) {
                    Jvm.warn().on(contextClass(contextSupplier), "Failure to dispatch message: " + name + " " + argArr[0], i);
                }
            });

        } else {
            ReadMarshallable arg;
            try {
                Constructor constructor = parameterType.getDeclaredConstructor();
                Jvm.setAccessible(constructor);
                arg = (ReadMarshallable) constructor.newInstance();
            } catch (Exception e) {
                try {
                    arg = (ReadMarshallable) OS.memory().allocateInstance(parameterType);
                } catch (InstantiationException e1) {
                    throw Jvm.rethrow(e1);
                }
            }
            @NotNull ReadMarshallable[] argArr = {arg};
            MethodWireKey key = createWireKey(m, name);
            wireParser.registerOnce(key, (s, v) -> {
                try {
                    if (Jvm.isDebug())
                        logMessage(s, v);

                    argArr[0] = v.object(checkRecycle(argArr[0]), parameterType);
                    Object invoke = invoke(contextSupplier.get(), m, argArr);
                    if (invoke != null)
                        context[0] = invoke;
                    else if (o2 != null)
                        context[0] = o2;
                } catch (Throwable t) {
                    Jvm.warn().on(contextClass(contextSupplier), "Failure to dispatch message: " + name + " " + argArr[0], t);
                }
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
            try {
                if (Jvm.isDebug())
                    logMessage(s, v);

                v.skipValue();

                Object invoke = invoke(contextSupplier.get(), m, NO_ARGS);
                if (invoke != null)
                    context[0] = invoke;
                if (o2 != null)
                    context[0] = o2;
            } catch (Exception i) {
                Jvm.warn().on(contextClass(contextSupplier), "Failure to dispatch message: " + name + "()", i);
            }
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
            try {
                if (Jvm.isDebug())
                    logMessage(s, v);

                v.sequence(args, sequenceReader);

                Object invoke = invoke(contextSupplier.get(), m, args);
                if (invoke != null)
                    context[0] = invoke;
                if (o2 != null)
                    context[0] = o2;
            } catch (Exception i) {
                Jvm.warn().on(contextClass(contextSupplier), "Failure to dispatch message: " + name + " " + Arrays.toString(args), i);
            }
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
            try {
                if (Jvm.isDebug())
                    logMessage(s, v);

                v.sequence(args, sequenceReader);
                if (args[0] == IGNORED) {
                    args[0] = null;
                    return;
                }
                Object invoke = invoke(contextSupplier.get(), m, args);
                if (invoke != null)
                    context[0] = invoke;
                if (o2 != null)
                    context[0] = o2;
            } catch (Exception i) {
                Jvm.warn().on(contextClass(contextSupplier), "Failure to dispatch message: " + name + " " + Arrays.toString(args), i);
            }
        });
    }

    protected Object invoke(Object o, @NotNull Method m, Object[] args) {
        try {
            if (methodReaderInterceptorReturns != null)
                return methodReaderInterceptorReturns.intercept(m, o, args, VanillaMethodReader::actualInvoke);
            else
                return m.invoke(o, args);

        } catch (InvocationTargetException | IllegalAccessException e) {
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
     * If we read a metadata message, true is returned even if it was ignored
     */
    public boolean readOne() {
        throwExceptionIfClosed();

        return readOne0();
    }

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

            messageHistory.reset(context.sourceId(), context.index());
            wireParser.accept(context.wire());
        }
        return true;
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
