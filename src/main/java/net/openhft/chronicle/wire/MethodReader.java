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
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.Closeable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

/*
 * Created by peter on 24/03/16.
 */
public class MethodReader implements Closeable {
    static final Object[] NO_ARGS = {};
    static final Logger LOGGER = LoggerFactory.getLogger(MethodReader.class);
    public static final String HISTORY = "history";
    private final MarshallableIn in;
    @NotNull
    private final WireParser<Void> wireParser;
    private boolean closeIn = false, closed;
    private MethodReaderInterceptor methodReaderInterceptor;

    public MethodReader(MarshallableIn in, boolean ignoreDefault, WireParselet defaultParselet, MethodReaderInterceptor methodReaderInterceptor, @NotNull Object... objects) {
        this.in = in;
        this.methodReaderInterceptor = methodReaderInterceptor;
        if (objects[0] instanceof WireParselet)
            defaultParselet = (WireParselet) objects[0];
        wireParser = WireParser.wireParser(defaultParselet);

        @NotNull Set<String> methodsHandled = new HashSet<>();
        for (@NotNull Object o : objects) {
            for (@NotNull Method m : o.getClass().getMethods()) {
                if (Modifier.isStatic(m.getModifiers()))
                    continue;
                if (ignoreDefault && m.getDeclaringClass().isInterface())
                    continue;

                try {
                    // skip Object defined methods.
                    Object.class.getMethod(m.getName(), m.getParameterTypes());
                    continue;
                } catch (NoSuchMethodException e) {
                    // not an Object method.
                }

                if (!methodsHandled.add(m.getName()))
                    continue;

                Class<?>[] parameterTypes = m.getParameterTypes();
                switch (parameterTypes.length) {
                    case 0:
                        addParseletForMethod(o, m);
                        break;
                    case 1:
                        addParseletForMethod(o, m, parameterTypes[0]);
                        break;
                    default:
                        addParseletForMethod(o, m, parameterTypes);
                        break;
                }
            }
        }
        if (wireParser.lookup(HISTORY) == null) {
            wireParser.register(() -> HISTORY, (s, v, $) -> v.marshallable(MessageHistory.get()));
        }
    }

    static void logMessage(@NotNull CharSequence s, @NotNull ValueIn v) {
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
        LOGGER.debug("read " + name + " - " + rest);
    }

    @NotNull
    public MethodReader closeIn(boolean closeIn) {
        this.closeIn = closeIn;
        return this;
    }

    private static Object actualInvoke(Method method, Object o, Object[] objects) throws InvocationTargetException {
        try {
            return method.invoke(o, objects);
        } catch (IllegalAccessException iae) {
            throw Jvm.rethrow(iae);
        }
    }

    public void addParseletForMethod(Object o, @NotNull Method m, Class<?> parameterType) {
        m.setAccessible(true); // turn of security check to make a little faster
        String name = m.getName();
        if (parameterType.isInterface() || !ReadMarshallable.class.isAssignableFrom(parameterType)) {
            @NotNull Object[] argArr = {null};
            wireParser.register(m::getName, (s, v, $) -> {
                try {
                    if (Jvm.isDebug())
                        logMessage(s, v);

                    argArr[0] = v.object(argArr[0], parameterType);
                    invoke(o, m, argArr);
                } catch (Exception i) {
                    Jvm.warn().on(o.getClass(), "Failure to dispatch message: " + name + " " + argArr[0], i);
                }
            });

        } else {
            ReadMarshallable arg;
            try {
                Constructor constructor = ((Class) parameterType).getDeclaredConstructor();
                constructor.setAccessible(true);
                arg = (ReadMarshallable) constructor.newInstance();
            } catch (Exception e) {
                try {
                    arg = (ReadMarshallable) OS.memory().allocateInstance((Class) parameterType);
                } catch (InstantiationException e1) {
                    throw Jvm.rethrow(e1);
                }
            }
            @NotNull ReadMarshallable[] argArr = {arg};
            wireParser.register(m::getName, (s, v, $) -> {
                try {
                    if (Jvm.isDebug() && LOGGER.isDebugEnabled())
                        logMessage(s, v);

                    argArr[0] = v.object(argArr[0], parameterType);
                    invoke(o, m, argArr);
                } catch (Throwable t) {
                    Jvm.warn().on(o.getClass(), "Failure to dispatch message: " + name + " " + argArr[0], t);
                }
            });
        }
    }

    public void addParseletForMethod(Object o, @NotNull Method m) {
        m.setAccessible(true); // turn of security check to make a little faster
        String name = m.getName();
        wireParser.register(m::getName, (s, v, $) -> {
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

    public void addParseletForMethod(Object o, @NotNull Method m, @NotNull Class[] parameterTypes) {
        m.setAccessible(true); // turn of security check to make a little faster
        @NotNull Object[] args = new Object[parameterTypes.length];
        @NotNull BiConsumer<Object[], ValueIn> sequenceReader = (a, v) -> {
            int i = 0;
            for (@NotNull Class clazz : parameterTypes) {
                a[i++] = v.object(clazz);
            }
        };
        String name = m.getName();
        wireParser.register(m::getName, (s, v, $) -> {
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

    private void invoke(Object o, @NotNull Method m, Object[] args) throws IllegalAccessException {
        try {
            if (methodReaderInterceptor != null)
                methodReaderInterceptor.intercept(m, o, args, MethodReader::actualInvoke);
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
     * @return true if there was a message, or false if not.
     */
    public boolean readOne() {
        MessageHistory history = MessageHistory.get();
        try (DocumentContext context = in.readingDocument()) {
            if (!context.isData())
                return false;
            history.reset(context.sourceId(), context.index());
            wireParser.accept(context.wire(), null);
        }
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

    public MethodReaderInterceptor methodReaderInterceptor() {
        return methodReaderInterceptor;
    }
}
