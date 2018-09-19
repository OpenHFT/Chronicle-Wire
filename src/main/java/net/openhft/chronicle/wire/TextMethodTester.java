package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/*
 * Created by Peter Lawrey on 17/05/2017.
 */
public class TextMethodTester<T> {
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
    private MethodReaderInterceptor methodReaderInterceptor;
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

        Wire wire2 = new TextWire(Bytes.allocateElasticDirect()).useTextDocuments().addTimeStamps(true);
        MethodWriterBuilder<T> methodWriterBuilder = wire2.methodWriterBuilder(outputClass)
                .methodWriterListener(methodWriterListener);
        if (genericEvent != null) methodWriterBuilder.genericEvent(genericEvent);
        T writer0 = methodWriterBuilder.build();
        T writer = retainLast == null
                ? writer0
                : cachedMethodWriter(writer0);
        Object component = componentFunction.apply(writer);
        Object[] components = component instanceof Object[]
                ? (Object[]) component
                : new Object[]{component};

        if (setup != null) {
            Wire wire0 = new TextWire(BytesUtil.readFile(setup));

            MethodReader reader0 = wire0.methodReaderBuilder()
                    .methodReaderInterceptor(methodReaderInterceptor)
                    .warnMissing(true)
                    .build(components);
            while (reader0.readOne()) {
                wire2.bytes().clear();
            }
            wire2.bytes().clear();
        }

        Wire wire = new TextWire(BytesUtil.readFile(input));

        // expected
        if (retainLast == null) {
            expected = BytesUtil.readFile(output).toString().trim().replace("\r", "");
        } else {
            Wire wireOut = new TextWire(BytesUtil.readFile(output));
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
            expected = expected2.toString().trim();
        }
        MethodReader reader = wire.methodReaderBuilder()
                .methodReaderInterceptor(methodReaderInterceptor)
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
        actual = wire2.toString().trim();
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() < start + timeoutMS) {
            if (actual.length() < expected.length())
                Jvm.pause(25);
            else
                break;
            actual = wire2.toString().trim();
        }
        if (afterRun != null) {
            expected = afterRun.apply(expected);
            actual = afterRun.apply(actual);
        }
        return this;
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

    public TextMethodTester<T> methodWriterListener(MethodWriterListener methodWriterListener) {
        this.methodWriterListener = methodWriterListener;
        return this;
    }

    public TextMethodTester<T> methodReaderInterceptor(MethodReaderInterceptor methodReaderInterceptor) {
        this.methodReaderInterceptor = methodReaderInterceptor;
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
