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
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.*;
import net.openhft.chronicle.core.onoes.ChainedExceptionHandler;
import net.openhft.chronicle.core.onoes.ExceptionHandler;
import net.openhft.chronicle.core.util.InvocationTargetRuntimeException;
import net.openhft.chronicle.wire.utils.YamlAgitator;
import net.openhft.chronicle.wire.utils.YamlTester;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class TextMethodTester<T> implements YamlTester {
    private static final boolean TESTS_INCLUDE_COMMENTS = Jvm.getBoolean("tests.include.comments", true);

    public static final boolean SINGLE_THREADED_CHECK_DISABLED = !Jvm.getBoolean("yaml.tester.single.threaded.check.enabled", false);


    private static final boolean DUMP_TESTS = Jvm.getBoolean("dump.tests");
    public static final Consumer<InvocationTargetRuntimeException> DEFAULT_INVOCATION_TARGET_RUNTIME_EXCEPTION_CONSUMER =
            e -> Jvm.warn().on(TextMethodTester.class, "Exception calling target method. Continuing", e);
    private final String input;
    private final Class<T> outputClass;
    private final Set<Class> additionalOutputClasses = new LinkedHashSet<>();
    private final Function<WireOut, T> outputFunction;
    private final String output;
    private final BiFunction<T, UpdateInterceptor, Object> componentFunction;
    private final boolean TEXT_AS_YAML = Jvm.getBoolean("wire.testAsYaml");
    private Function<T, ExceptionHandler> exceptionHandlerFunction;
    private BiConsumer<MethodReader, T> exceptionHandlerSetup;
    private String genericEvent;
    private List<String> setups;
    private Function<String, String> inputFunction;
    private Function<String, String> afterRun;
    private String expected;
    private String actual;
    private String[] retainLast;
    private MethodReaderInterceptorReturns methodReaderInterceptorReturns;
    private long timeoutMS = 25;
    private UpdateInterceptor updateInterceptor;
    private Consumer<InvocationTargetRuntimeException> onInvocationException;
    private boolean exceptionHandlerFunctionAndLog;
    private Predicate<String> testFilter = s -> true;

    public TextMethodTester(String input, Function<T, Object> componentFunction, Class<T> outputClass, String output) {
        this(input, (out, ui) -> componentFunction.apply(out), outputClass, output);
    }

    public TextMethodTester(String input, BiFunction<T, UpdateInterceptor, Object> componentFunction, Class<T> outputClass, String output) {
        this(input, componentFunction, null, outputClass, output);
    }

    public TextMethodTester(String input, Function<T, Object> componentFunction, Function<WireOut, T> outputFunction, String output) {
        this(input, (out, ui) -> componentFunction.apply(out), outputFunction, null, output);
    }

    private TextMethodTester(String input, BiFunction<T, UpdateInterceptor, Object> componentFunction, Function<WireOut, T> outputFunction, Class<T> outputClass, String output) {
        this.input = input;
        this.componentFunction = componentFunction;
        this.outputFunction = outputFunction;
        this.outputClass = outputClass;
        this.output = output;

        this.setups = Collections.emptyList();
        this.onInvocationException = DEFAULT_INVOCATION_TARGET_RUNTIME_EXCEPTION_CONSUMER;
    }

    public TextMethodTester<T> addOutputClass(Class outputClass) {
        additionalOutputClasses.add(outputClass);
        return this;
    }

    public static boolean resourceExists(String resourceName) {
        try {
            return new File(resourceName).exists() || IOTools.urlFor(TextMethodTester.class, resourceName) != null;
        } catch (FileNotFoundException ignored) {
            return false;
        }
    }

    public String[] retainLast() {
        return retainLast;
    }

    @NotNull
    public TextMethodTester<T> retainLast(String... retainLast) {
        this.retainLast = retainLast;
        return this;
    }

    public String setup() {
        if (setups.size() != 1)
            throw new IllegalStateException();
        return setups.get(0);
    }

    @NotNull
    public TextMethodTester<T> setup(@Nullable String setup) {
        this.setups = (setup == null) ? Collections.emptyList() : Collections.singletonList(setup);
        return this;
    }

    @NotNull
    public TextMethodTester<T> setups(@NotNull List<String> setups) {
        this.setups = setups;
        return this;
    }

    public Function<String, String> afterRun() {
        return afterRun;
    }

    @NotNull
    public TextMethodTester<T> afterRun(Function<String, String> afterRun) {
        this.afterRun = afterRun;
        return this;
    }

    public BiConsumer<MethodReader, T> exceptionHandlerSetup() {
        return exceptionHandlerSetup;
    }

    public TextMethodTester<T> exceptionHandlerSetup(BiConsumer<MethodReader, T> exceptionHandlerSetup) {
        this.exceptionHandlerSetup = exceptionHandlerSetup;
        return this;
    }

    public String genericEvent() {
        return genericEvent;
    }

    public TextMethodTester<T> genericEvent(String genericEvent) {
        this.genericEvent = genericEvent;
        return this;
    }

    public Consumer<InvocationTargetRuntimeException> onInvocationException() {
        return onInvocationException;
    }

    public TextMethodTester<T> onInvocationException(Consumer<InvocationTargetRuntimeException> onInvocationException) {
        this.onInvocationException = onInvocationException;
        return this;
    }

    @NotNull
    public TextMethodTester<T> run() throws IOException {
        OnHeapBytes b = Bytes.allocateElasticOnHeap();
        b.singleThreadedCheckDisabled(SINGLE_THREADED_CHECK_DISABLED);
        Wire wireOut = createWire(b);

        T writer0;
        if (outputClass != null) {
            MethodWriterBuilder<T> methodWriterBuilder = wireOut.methodWriterBuilder(outputClass);
            additionalOutputClasses.forEach(((VanillaMethodWriterBuilder) methodWriterBuilder)::addInterface);
            if (updateInterceptor != null)
                methodWriterBuilder.updateInterceptor(updateInterceptor);

            if (genericEvent != null) methodWriterBuilder.genericEvent(genericEvent);

            writer0 = methodWriterBuilder.get();
        } else {
            writer0 = outputFunction.apply(wireOut);
        }
        T writer = retainLast == null
                ? writer0
                : cachedMethodWriter(writer0);
        Object component = componentFunction.apply(writer, updateInterceptor);
        Object[] components = component instanceof Object[]
                ? (Object[]) component
                : new Object[]{component};

        String setupNotFound = "";
        final Class<?> clazz = outputClass == null ? getClass() : outputClass;
        for (String setup : setups) {
            try {
                byte[] setupBytes = IOTools.readFile(clazz, setup);
                Wire wire0 = createWire(setupBytes);
                MethodReader reader0 = wire0.methodReaderBuilder()
                        .methodReaderInterceptorReturns(methodReaderInterceptorReturns)
                        .warnMissing(true)
                        .build(components);
                while (readOne(reader0, null)) {
                    wireOut.bytes().clear();
                }
                wireOut.bytes().clear();
            } catch (FileNotFoundException ignored) {
                setupNotFound = setup + " not found";
            }
        }

        if (component instanceof PostSetup)
            ((PostSetup) component).postSetup();

        if (DUMP_TESTS)
            System.out.println("input: " + input);

        byte[] inputBytes = input.startsWith("=")
                ? input.substring(1).trim().getBytes()
                : IOTools.readFile(clazz, input);

        Wire wire = createWire(inputBytes);
        if (TESTS_INCLUDE_COMMENTS)
            wire.commentListener(wireOut::writeComment);

        // expected
        if (retainLast == null) {
            if (REGRESS_TESTS) {
                expected = "";
            } else {
                String outStr = output.startsWith("=")
                        ? output.substring(1)
                        : new String(IOTools.readFile(clazz, output), StandardCharsets.ISO_8859_1);
                expected = outStr.trim().replace("\r", "");
            }
        } else {
            ValidatableUtil.startValidateDisabled();
            try {
                expected = loadLastValues().toString().trim();
            } finally {
                ValidatableUtil.endValidateDisabled();
            }
        }
        String originalExpected = expected;
        boolean[] sepOnNext = {true};

        ExceptionHandler exceptionHandler = null;
        ExceptionHandler warn = Jvm.warn();
        ExceptionHandler error = Jvm.error();
        ExceptionHandler debug = Jvm.debug();
        if (exceptionHandlerFunction != null) {
            exceptionHandler = createExceptionHandler(writer0, warn, error);
        }
        MethodReader reader = wire.methodReaderBuilder()
                .methodReaderInterceptorReturns((Method m, Object o, Object[] args, net.openhft.chronicle.bytes.Invocation invocation) -> {
                    if (sepOnNext[0])
                        wireOut.bytes().append("---\n");
                    sepOnNext[0] = !(m.getReturnType().isInterface());
                    if (methodReaderInterceptorReturns == null)
                        return invocation.invoke(m, o, args);
                    return methodReaderInterceptorReturns.intercept(m, o, args, invocation);
                })
                .warnMissing(true)
                .build(components);

        if (exceptionHandlerSetup != null)
            exceptionHandlerSetup.accept(reader, writer);

        long pos = -1;
        boolean ok = false;
        try {
            while (readOne(reader, exceptionHandler)) {
                if (pos == wire.bytes().readPosition()) {
                    Jvm.warn().on(getClass(), "Bailing out of malformed message");
                    break;
                }
                Bytes<?> bytes2 = wireOut.bytes();
                if (retainLast == null) {
                    if (bytes2.writePosition() > 0) {
                        int last = bytes2.peekUnsignedByte(bytes2.writePosition() - 1);
                        if (last >= ' ')
                            bytes2.append('\n');
                    }
                }
                pos = bytes2.readPosition();
            }
            ok = true;
        } finally {
            if (exceptionHandlerFunction != null)
                Jvm.setExceptionHandlers(error, warn, debug);

            if (!ok)
                System.err.println("Unable to parse\n" + new String(inputBytes, StandardCharsets.UTF_8));
        }
        if (retainLast != null)
            wireOut.bytes().clear();

        if (retainLast != null) {
            CachedInvocationHandler invocationHandler =
                    (CachedInvocationHandler) Proxy.getInvocationHandler(writer);
            try {
                invocationHandler.flush();
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        Closeable.closeQuietly(components);

        actual = wireOut.toString().trim();
        if (REGRESS_TESTS && !output.startsWith("=")) {
            Jvm.pause(100);
            expected = actual = wireOut.toString().trim();
        } else {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() < start + timeoutMS) {
                if (actual.length() < expected.length())
                    Jvm.pause(25);
                else
                    break;
                actual = wireOut.toString().trim();
            }
        }

        if (afterRun != null) {
            expected = afterRun.apply(expected);
            actual = afterRun.apply(actual);
        }
        if (OS.isWindows()) {
            expected = expected.replace("\r\n", "\n");
            actual = actual.replace("\r\n", "\n");
        }
        if (REGRESS_TESTS && !originalExpected.equals(expected)) {
            updateOutput();
        }
        // add a warning if they don't match and there was a setup missing.
        if (!expected.trim().equals(actual.trim()) && !setupNotFound.isEmpty())
            Jvm.warn().on(getClass(), setupNotFound);
        return this;
    }

    private void updateOutput() throws IOException {
        String output = replaceTargetWithSource(this.output);
        String output2;
        try {
            output2 = BytesUtil.findFile(output);
        } catch (FileNotFoundException fnfe) {
            File out2 = new File(this.output);
            File out = new File(out2.getParentFile(), "out.yaml");
            try {
                String output2dir = BytesUtil.findFile(replaceTargetWithSource(out.getPath()));
                output2 = new File(new File(output2dir).getParentFile(), out2.getName()).getPath();
            } catch (FileNotFoundException e2) {
                throw fnfe;
            }
        }
        String actual2 = actual.endsWith("\n") ? actual : (actual + "\n");
        if (!testFilter.test(actual2)) {
            System.err.println("The expected output for " + output2 + " has been drops as it is too similar to previous results");
            return;
        }
        System.err.println("The expected output for " + output2 + " has been updated, check your commits");

        try (FileWriter fw = new FileWriter(output2)) {
            if (OS.isWindows())
                actual2 = actual2.replace("\n", "\r\n");
            fw.write(actual2);
        }
    }

    private ExceptionHandler createExceptionHandler(T writer0, ExceptionHandler warn, ExceptionHandler error) {
        ExceptionHandler exceptionHandler;
        exceptionHandler = exceptionHandlerFunction.apply(writer0);

        if (exceptionHandlerFunctionAndLog) {
            if (onInvocationException == DEFAULT_INVOCATION_TARGET_RUNTIME_EXCEPTION_CONSUMER) {
                ChainedExceptionHandler eh2 = new ChainedExceptionHandler(error, exceptionHandler);
                Consumer<InvocationTargetRuntimeException> invocationException =
                        er -> eh2.on(LoggerFactory.getLogger(classNameFor(er.getCause())), "Unhandled Exception", er.getCause());
                onInvocationException = invocationException;
            }

            Jvm.setExceptionHandlers(
                    new ChainedExceptionHandler(error, exceptionHandler),
                    new ChainedExceptionHandler(warn, exceptionHandler),
                    null);
        } else {
            if (onInvocationException == DEFAULT_INVOCATION_TARGET_RUNTIME_EXCEPTION_CONSUMER) {
                ExceptionHandler eh = exceptionHandler;
                Consumer<InvocationTargetRuntimeException> invocationException =
                        er -> eh.on(LoggerFactory.getLogger(classNameFor(er.getCause())), "Unhandled Exception", er.getCause());
                onInvocationException = invocationException;
            }
            Jvm.setExceptionHandlers(
                    exceptionHandler,
                    exceptionHandler,
                    null);
        }
        return exceptionHandler;
    }

    @Override
    public Map<String, String> agitate(YamlAgitator agitator) throws IORuntimeException {
        try {
            final Class<?> clazz = outputClass == null ? getClass() : outputClass;
            String yaml = input.startsWith("=")
                    ? input.substring(1)
                    : new String(IOTools.readFile(clazz, input), StandardCharsets.UTF_8);
            return agitator.generateInputs(yaml);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    public boolean readOne(MethodReader reader0, ExceptionHandler exceptionHandler) {
        try {
            return reader0.readOne();
        } catch (InvocationTargetRuntimeException e) {
            this.onInvocationException.accept(e);

        } catch (Throwable t) {
            if (exceptionHandler == null)
                throw t;
            exceptionHandler.on(LoggerFactory.getLogger(classNameFor(t)), t.toString());
        }
        return true;
    }

    @NotNull
    private static String classNameFor(Throwable t) {
        StackTraceElement[] stackTrace = t.getStackTrace();
        return stackTrace.length == 0 ? "TextMethodTester" : stackTrace[0].getClassName();
    }

    private String replaceTargetWithSource(String replace) {
        return replace
                .replace('\\', '/')
                .replace("/target/test-classes/", "/src/test/resources/");
    }

    protected Wire createWire(byte[] byteArray) {
        final Bytes<?> bytes;
        if (inputFunction == null) {
            bytes = Bytes.wrapForRead(byteArray);
        } else {
            bytes = Bytes.from(inputFunction.apply(new String(byteArray, StandardCharsets.ISO_8859_1)));
        }
        return createWire(bytes);
    }

    protected Wire createWire(Bytes<?> bytes) {
        return TEXT_AS_YAML
                ? new YamlWire(bytes).useTextDocuments().addTimeStamps(true)
                : new TextWire(bytes).useTextDocuments().addTimeStamps(true);
    }

    @NotNull
    protected StringBuilder loadLastValues() throws IOException, InvalidMarshallableException {
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
            BytesStore bytesStore = wireOut.bytes().subBytes(start, end - start);
            events.put(key.toString(), bytesStore.toString().trim());
            bytesStore.releaseLast();
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
        if (expected == null)
            try {
                run();
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
        return expected;
    }

    public String actual() {
        if (actual == null)
            try {
                run();
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
        return actual;
    }

    public TextMethodTester<T> updateInterceptor(UpdateInterceptor updateInterceptor) {
        this.updateInterceptor = updateInterceptor;
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

    public TextMethodTester<T> exceptionHandlerFunction(Function<T, ExceptionHandler> exceptionHandlerFunction) {
        this.exceptionHandlerFunction = exceptionHandlerFunction;
        return this;
    }

    public TextMethodTester<T> exceptionHandlerFunctionAndLog(boolean exceptionHandlerFunctionAndLog) {
        this.exceptionHandlerFunctionAndLog = exceptionHandlerFunctionAndLog;
        return this;
    }

    public TextMethodTester<T> testFilter(Predicate<String> testFilter) {
        this.testFilter = testFilter;
        return this;
    }

    public TextMethodTester<T> inputFunction(Function<String, String> inputFunction) {
        this.inputFunction = inputFunction;
        return this;
    }

    public interface PostSetup {
        void postSetup();
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
