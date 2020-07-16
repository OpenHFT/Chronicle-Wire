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

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.Closeable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

@SuppressWarnings({"rawtypes", "unchecked"})
public class TextMethodTester<T> {
    private static final boolean REGRESS_TESTS = Jvm.getBoolean("regress.tests");
    private final String input;
    private final Class<T> outputClass;
    private final String output;
    private final Function<T, Object> componentFunction;
    private BiConsumer<MethodReader, T> exceptionHandlerSetup;
    private String genericEvent;

    private String setup;
    private Function<String, String> afterRun;

    private String expected;
    private String actual;
    private String[] retainLast;
    private MethodWriterListener methodWriterListener;
    private MethodReaderInterceptorReturns methodReaderInterceptorReturns;
    private long timeoutMS = 25;

    public TextMethodTester(String input, Function<T, Object> componentFunction, Class<T> outputClass, String output) {
        this.input = input;
        this.outputClass = outputClass;
        this.output = output;
        this.componentFunction = componentFunction;
    }

    public String[] retainLast() {
        return retainLast;
    }

    @NotNull
    public TextMethodTester retainLast(String... retainLast) {
        this.retainLast = retainLast;
        return this;
    }

    public String setup() {
        return setup;
    }

    @NotNull
    public TextMethodTester setup(String setup) {
        this.setup = setup;
        return this;
    }

    public Function<String, String> afterRun() {
        return afterRun;
    }

    @NotNull
    public TextMethodTester afterRun(Function<String, String> afterRun) {
        this.afterRun = afterRun;
        return this;
    }

    public BiConsumer<MethodReader, T> exceptionHandlerSetup() {
        return exceptionHandlerSetup;
    }

    public TextMethodTester exceptionHandlerSetup(BiConsumer<MethodReader, T> exceptionHandlerSetup) {
        this.exceptionHandlerSetup = exceptionHandlerSetup;
        return this;
    }

    public String genericEvent() {
        return genericEvent;
    }

    public TextMethodTester genericEvent(String genericEvent) {
        this.genericEvent = genericEvent;
        return this;
    }

    @NotNull
    public TextMethodTester run() throws IOException {
        if (REGRESS_TESTS)
            System.err.println("NOTE: Regressing tests, please check your commits");
        Wire wire2 = createWire(Bytes.allocateElasticOnHeap());

        MethodWriterBuilder<T> methodWriterBuilder = wire2.methodWriterBuilder(outputClass);
      //  if (methodWriterListener != null) {

            //     MethodWriterInterceptorReturns interceptorReturns = (method, args, invoker) -> {
            //      methodWriterListener.onWrite(method.getName(), args);
            //      return invoker.apply(method, args);
            //  };
            //   methodWriterBuilder.updateInterceptor(this::updateInterceptor);

        //}
        if (updateInterceptor != null)
            methodWriterBuilder.updateInterceptor(updateInterceptor);

        if (genericEvent != null) methodWriterBuilder.genericEvent(genericEvent);

        T writer0 = methodWriterBuilder.get();
        T writer = retainLast == null
                ? writer0
                : cachedMethodWriter(writer0);
        Object component = componentFunction.apply(writer);
        Object[] components = component instanceof Object[]
                ? (Object[]) component
                : new Object[]{component};

        if (setup != null) {
            Wire wire0 = createWire(BytesUtil.readFile(setup));

            MethodReader reader0 = wire0.methodReaderBuilder()
                    .methodReaderInterceptorReturns(methodReaderInterceptorReturns)
                    .warnMissing(true)
                    .build(components);
            while (reader0.readOne()) {
                wire2.bytes().clear();
            }
            wire2.bytes().clear();
        }

        Wire wire = createWire(BytesUtil.readFile(input));

        if (!REGRESS_TESTS) {
            // expected
            if (retainLast == null) {
                expected = BytesUtil.readFile(output).toString().trim().replace("\r", "");
            } else {
                expected = loadLastValues().toString().trim();
            }
        }
        MethodReader reader = wire.methodReaderBuilder()
                .methodReaderInterceptorReturns(methodReaderInterceptorReturns)
                .warnMissing(true)
                .build(components);

        if (exceptionHandlerSetup != null)
            exceptionHandlerSetup.accept(reader, writer);

//        long pos = wire2.bytes().writePosition();
        TextMethodWriterInvocationHandler.ENABLE_EOD = false;
        try {
            long pos = -1;
            while (reader.readOne()) {
                if (pos == wire.bytes().readPosition()) {
                    Jvm.warn().on(getClass(), "Bailing out of malformed message");
                    break;
                }
                if (retainLast == null) {
                    Bytes<?> bytes = wire2.bytes();
                    int last = bytes.peekUnsignedByte(bytes.writePosition() - 1);
                    if (last >= ' ')
                        bytes.append('\n');
                    bytes.append("---\n");
                }
                pos = wire2.bytes().readPosition();
            }
            if (retainLast != null)
                wire2.bytes().clear();

            if (retainLast != null) {
                CachedInvocationHandler invocationHandler =
                        (CachedInvocationHandler) Proxy.getInvocationHandler(writer);
                try {
                    invocationHandler.flush();
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
        } finally {
            TextMethodWriterInvocationHandler.ENABLE_EOD = true;

        }

        if (component instanceof Closeable)
            Closeable.closeQuietly(components);

        actual = wire2.toString().trim();
        if (REGRESS_TESTS) {
            Jvm.pause(100);
            expected = actual = wire2.toString().trim();
        } else {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() < start + timeoutMS) {
                if (actual.length() < expected.length())
                    Jvm.pause(25);
                else
                    break;
                actual = wire2.toString().trim();
            }
        }

        if (afterRun != null) {
            expected = afterRun.apply(expected);
            actual = afterRun.apply(actual);
        }
        if (REGRESS_TESTS) {
            String output2;
            try {
                output2 = BytesUtil.findFile(output);
            } catch (FileNotFoundException fnfe) {
                try {
                    output2 = BytesUtil.findFile(input.replace("in.yaml", "out.yaml"));
                } catch (FileNotFoundException e) {
                    throw fnfe;
                }
            }

            try (FileWriter fw = new FileWriter(output2)) {
                String actual2 = actual.endsWith("\n") ? actual : (actual + "\n");
                if (OS.isWindows())
                    actual2 = actual2.replace("\n", "\r\n");
                fw.write(actual2);
            }
        }
        return this;
    }

    protected Wire createWire(Bytes bytes) {
        return new TextWire(bytes).useTextDocuments().addTimeStamps(true);
    }

    @NotNull
    protected StringBuilder loadLastValues() throws IOException {
        Wire wireOut = createWire(BytesUtil.readFile(output));
        Map<String, String> events = new TreeMap<>();
        consumeDocumentSeparator(wireOut);
        while (wireOut.hasMore()) {
            StringBuilder event = new StringBuilder();
            long start = wireOut.bytes().readPosition();
            Map<String, Object> m = wireOut.read(event).marshallableAsMap(String.class, Object.class);
            assert m != null;
            StringBuilder key = new StringBuilder(event);
            for (String s : retainLast) {
                key.append(",").append(m.get(s));
            }
            long end = wireOut.bytes().readPosition();
            events.put(key.toString(), wireOut.bytes().subBytes(start, end - start).toString().trim());
            consumeDocumentSeparator(wireOut);
        }
        StringBuilder expected2 = new StringBuilder();
        for (String s : events.values()) {
            expected2.append(s.replace("\r", "")).append("\n");
        }
        return expected2;
    }

    private void consumeDocumentSeparator(@NotNull Wire wireOut) {
        if (wireOut.bytes().peekUnsignedByte() == '-') {
            wireOut.bytes().readSkip(3);
        }
    }

    @NotNull
    private T cachedMethodWriter(T writer0) {
        Class[] interfaces = {outputClass};
        return (T) Proxy.newProxyInstance(outputClass.getClassLoader(), interfaces, new CachedInvocationHandler(writer0));
    }

    public String expected() {
        return expected;
    }

    public String actual() {
        return actual;
    }

    @Deprecated
    public TextMethodTester<T> methodWriterListener(MethodWriterListener methodWriterListener) {
        this.methodWriterListener = methodWriterListener;
        return this;
    }

    private UpdateInterceptor updateInterceptor;

    public TextMethodTester<T> updateInterceptor(UpdateInterceptor updateInterceptor) {
        this.updateInterceptor = updateInterceptor;
        return this;
    }

    @Deprecated
    public TextMethodTester<T> methodReaderInterceptor(MethodReaderInterceptor methodReaderInterceptor) {
        this.methodReaderInterceptorReturns = (m, o, a, i) -> {
            methodReaderInterceptor.intercept(m, o, a, i);
            return null;
        };
        return this;
    }

    public TextMethodTester<T> methodReaderInterceptorReturns(MethodReaderInterceptorReturns methodReaderInterceptorReturns) {
        this.methodReaderInterceptorReturns = methodReaderInterceptorReturns;
        return this;
    }

    public TextMethodTester<T> timeoutMS(long timeoutMS) {
        this.timeoutMS = timeoutMS;
        return this;
    }

    @Deprecated(/* used by one client*/)
    static class Invocation {
        Method method;
        Object[] args;

        public Invocation(Method method, Object[] args) {
            this.method = method;
            this.args = args;
        }
    }

    @Deprecated(/* used by one client*/)
    class CachedInvocationHandler implements InvocationHandler {
        private final Map<String, Invocation> cache = new TreeMap<>();
        private final T writer0;

        public CachedInvocationHandler(T writer0) {
            this.writer0 = writer0;
        }

        @Nullable
        @Override
        public Object invoke(Object proxy, @NotNull Method method, @Nullable Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }

            if (args != null && args.length == 1 && args[0] instanceof Marshallable) {
                StringBuilder key = new StringBuilder();
                key.append(method.getName());
                Marshallable m = (Marshallable) args[0];
                try {
                    for (String s : retainLast) {
                        key.append(",").append(m.getField(s, Object.class));
                    }
                } catch (NoSuchFieldException e) {
                    // move on
                }
                args[0] = m.deepCopy();
                cache.put(key.toString(), new Invocation(method, args));
            } else {
                method.invoke(writer0, args);
            }
            return null;
        }

        public void flush() throws InvocationTargetException, IllegalAccessException {
            for (Invocation invocation : cache.values()) {
                invocation.method.invoke(writer0, invocation.args);
            }
        }
    }
}
