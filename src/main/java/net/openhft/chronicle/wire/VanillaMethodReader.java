/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import net.openhft.chronicle.bytes.MethodReaderInterceptor;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.util.Annotations;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import static net.openhft.chronicle.wire.VanillaWireParser.SKIP_READABLE_BYTES;

/*
 * Created by Peter Lawrey on 24/03/16.
 */
public class VanillaMethodReader implements MethodReader {

    static final Object[] NO_ARGS = {};
    static final Logger LOGGER = LoggerFactory.getLogger(VanillaMethodReader.class);
    static final Object IGNORED = new Object(); // object used to flag that the call should be ignored.
    private static final String[] metaIgnoreList = {"header", "index", "index2index", "roll"};
    private final MarshallableIn in;
    @NotNull
    private final WireParser wireParser;
    private final MessageHistory messageHistory = MessageHistory.get();
    private boolean closeIn = false, closed;
    private MethodReaderInterceptor methodReaderInterceptor;

    public VanillaMethodReader(MarshallableIn in,
                               boolean ignoreDefault,
                               WireParselet defaultParselet,
                               MethodReaderInterceptor methodReaderInterceptor,
                               @NotNull Object... objects) {
        this(in, ignoreDefault, defaultParselet, SKIP_READABLE_BYTES, methodReaderInterceptor, objects);
    }

    public VanillaMethodReader(MarshallableIn in,
                               boolean ignoreDefault,
                               WireParselet defaultParselet,
                               FieldNumberParselet fieldNumberParselet,
                               MethodReaderInterceptor methodReaderInterceptor,
                               @NotNull Object... objects) {
        this.in = in;
        this.methodReaderInterceptor = methodReaderInterceptor;
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
            for (@NotNull Method m : o.getClass().getMethods()) {
                if (Modifier.isStatic(m.getModifiers()))
                    continue;
                if (ignoreDefault && m.getDeclaringClass().isInterface())
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
                    Jvm.warn().on(getClass(), "Unable to support overloaded methods, ignoring one of " + m.getName());
                    continue;
                }

                Class<?>[] parameterTypes = m.getParameterTypes();
                switch (parameterTypes.length) {
                    case 0:
                        addParseletForMethod(o, m);
                        break;
                    case 1:
                        addParseletForMethod(o, m, parameterTypes[0]);
                        break;
                    default:
                        if (methodFilterOnFirstArg == null)
                            addParseletForMethod(o, m, parameterTypes);
                        else
                            addParseletForMethod(o, m, parameterTypes, methodFilterOnFirstArg);
                        break;
                }
            }
        }
        if (wireParser.lookup(HISTORY) == null) {
            wireParser.registerOnce(() -> HISTORY, (s, v) -> v.marshallable(messageHistory));
        }
    }

    private static Object actualInvoke(Method method, Object o, Object[] objects) throws InvocationTargetException {
        try {
            return method.invoke(o, objects);
        } catch (IllegalAccessException iae) {
            throw Jvm.rethrow(iae);
        }
    }

    private static void invokeMethodWithOneLong(Object o, @NotNull Method m, String name, MethodHandle mh, Object[] argArr, CharSequence s, ValueIn v, MethodReaderInterceptor methodReaderInterceptor) {
        try {
            if (Jvm.isDebug())
                logMessage(s, v);

            long arg = v.int64();
            try {
                if (methodReaderInterceptor != null) {
                    argArr[0] = arg;
                    methodReaderInterceptor.intercept(m, o, argArr, VanillaMethodReader::actualInvoke);

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

    static void logMessage(@NotNull CharSequence s, @NotNull ValueIn v) {
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
            bytes.release();

        } else {
            rest = v.toString();
        }
        // TextWire.toString has an \n at the end
        if (rest.endsWith("\n"))
            rest = rest.substring(0, rest.length() - 1);
        LOGGER.debug("read " + name + " - " + rest);
    }

    @NotNull
    public VanillaMethodReader closeIn(boolean closeIn) {
        this.closeIn = closeIn;
        return this;
    }

    // one arg
    public void addParseletForMethod(Object o, @NotNull Method m, Class<?> parameterType) {
        m.setAccessible(true); // turn of security check to make a little faster
        String name = m.getName();
        if (parameterType == long.class) {
            try {
                MethodHandle mh = MethodHandles.lookup().unreflect(m).bindTo(o);
                @NotNull Object[] argArr = {null};
                MethodWireKey key = createWireKey(m, name);
                wireParser.registerOnce(key, (s, v) -> invokeMethodWithOneLong(o, m, name, mh, argArr, s, v, methodReaderInterceptor));
            } catch (IllegalAccessException e) {
                Jvm.warn().on(o.getClass(), "Unable to unreflect " + m, e);
            }

        } else if (parameterType.isInterface() || !ReadMarshallable.class.isAssignableFrom(parameterType)) {
            @NotNull Object[] argArr = {null};
            MethodWireKey key = createWireKey(m, name);
            wireParser.registerOnce(key, (s, v) -> {
                try {
                    if (Jvm.isDebug())
                        logMessage(s, v);

                    argArr[0] = v.object(checkRecycle(argArr[0]), parameterType);
                    invoke(o, m, argArr);
                } catch (Exception i) {
                    Jvm.warn().on(o.getClass(), "Failure to dispatch message: " + name + " " + argArr[0], i);
                }
            });

        } else {
            ReadMarshallable arg;
            try {
                Constructor constructor = parameterType.getDeclaredConstructor();
                constructor.setAccessible(true);
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
                    invoke(o, m, argArr);
                } catch (Throwable t) {
                    Jvm.warn().on(o.getClass(), "Failure to dispatch message: " + name + " " + argArr[0], t);
                }
            });
        }
    }

    // no args
    public void addParseletForMethod(Object o, @NotNull Method m) {
        m.setAccessible(true); // turn of security check to make a little faster
        String name = m.getName();
        MethodWireKey key = createWireKey(m, name);
        wireParser.registerOnce(key, (s, v) -> {
            try {
                if (Jvm.isDebug())
                    logMessage(s, v);

                v.skipValue();

                invoke(o, m, NO_ARGS);
            } catch (Exception i) {
                Jvm.warn().on(o.getClass(), "Failure to dispatch message: " + name + "()", i);
            }
        });
    }

    @NotNull
    private MethodWireKey createWireKey(@NotNull Method m, String name) {
        MethodId annotation = Annotations.getAnnotation(m, MethodId.class);
        return new MethodWireKey(name, annotation == null ? name.hashCode() : Maths.toUInt31(annotation.value()));
    }

    public void addParseletForMethod(Object o, @NotNull Method m, @NotNull Class[] parameterTypes) {
        m.setAccessible(true); // turn of security check to make a little faster
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

                invoke(o, m, args);
            } catch (Exception i) {
                Jvm.warn().on(o.getClass(), "Failure to dispatch message: " + name + " " + Arrays.toString(args), i);
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

    public void addParseletForMethod(Object o, @NotNull Method m, @NotNull Class[] parameterTypes, MethodFilterOnFirstArg methodFilterOnFirstArg) {
        m.setAccessible(true); // turn of security check to make a little faster
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
                invoke(o, m, args);
            } catch (Exception i) {
                Jvm.warn().on(o.getClass(), "Failure to dispatch message: " + name + " " + Arrays.toString(args), i);
            }
        });
    }

    private void invoke(Object o, @NotNull Method m, Object[] args) throws IllegalAccessException {
        try {
            if (methodReaderInterceptor != null)
                methodReaderInterceptor.intercept(m, o, args, VanillaMethodReader::actualInvoke);
            else
                m.invoke(o, args);

        } catch (InvocationTargetException | IllegalAccessException e) {
            Throwable cause = e.getCause();
            String msg = "Failure to dispatch message: " + m.getName() + " " + Arrays.asList(args);
            if (cause instanceof IllegalArgumentException)
                Jvm.warn().on(o.getClass(), msg + " " + cause);
            else
                Jvm.warn().on(o.getClass(), msg, cause);
        }
    }

    /**
     * reads one message
     *
     * @return true if there was a message, or false if no more data is available.
     */
    public boolean readOne() {
        for (; ; ) {

            try (DocumentContext context = in.readingDocument()) {
                if (!context.isPresent())
                    return false;
                if (context.isMetaData()) {
                    StringBuilder sb = Wires.acquireStringBuilder();

                    long r = context.wire().bytes().readPosition();
                    try {
                        context.wire().readEventName(sb);

                        for (String s : metaIgnoreList) {
                            // we wish to ignore our system meta data field
                            if (s.contentEquals(sb))
                                return false;
                        }
                    } finally {
                        // roll back position to where is was before we read the SB
                        context.wire().bytes().readPosition(r);
                    }

                    wireParser.accept(context.wire());

                    return true;
                }
                if (!context.isData())
                    continue;
                MessageHistory history = messageHistory;
                history.reset(context.sourceId(), context.index());
                wireParser.accept(context.wire());
            }
            return true;
        }

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

    public MethodReaderInterceptor methodReaderInterceptor() {
        return methodReaderInterceptor;
    }
}
