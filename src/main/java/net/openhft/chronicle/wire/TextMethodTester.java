package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Created by peter on 17/05/2017.
 */
public class TextMethodTester<T> {
    private final String input;
    private final Class<T> outputClass;
    private final String output;
    private final Function<T, Object> componentFunction;

    private String setup;
    private Function<String, String> afterRun;

    private String expected;
    private String actual;
    private String[] retainLast;

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

    @NotNull
    public TextMethodTester run() throws IOException {

        Wire wire2 = new TextWire(Bytes.allocateElasticDirect()).useTextDocuments();
        T writer0 = wire2.methodWriter(outputClass);
        T writer = retainLast == null ? writer0 : cachedMethodWriter(writer0);
        Object component = componentFunction.apply(writer);
        Object[] components = component instanceof Object[]
                ? (Object[]) component
                : new Object[]{component};

        if (setup != null) {
            Wire wire0 = new TextWire(BytesUtil.readFile(setup)).useTextDocuments();

            MethodReader reader0 = wire0.methodReader(components);
            while (reader0.readOne()) {
                wire2.bytes().clear();
            }
            wire2.bytes().clear();
        }

        Wire wire = new TextWire(BytesUtil.readFile(input)).useTextDocuments();

        // expected
        if (retainLast == null) {
            expected = BytesUtil.readFile(output).toString().trim().replace("\r", "");
        } else {
            Wire wireOut = new TextWire(BytesUtil.readFile(output));
            Map<String, String> events = new TreeMap<>();
            consumeDocumentSeperator(wireOut);
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
                consumeDocumentSeperator(wireOut);
            }
            StringBuilder expected2 = new StringBuilder();
            for (String s : events.values()) {
                expected2.append(s.replace("\r", "")).append("\n");
            }
            expected = expected2.toString().trim();
        }
        MethodReader reader = wire.methodReader(components);

        long pos = wire2.bytes().writePosition();
        while (reader.readOne()) {
            if (retainLast == null || pos != wire2.bytes().writePosition())
                wire2.bytes().append("---\n");
            pos = wire2.bytes().writePosition();
        }
        if (retainLast != null) {
            CachedInvocationHandler invocationHandler =
                    (CachedInvocationHandler) Proxy.getInvocationHandler(writer);
            try {
                invocationHandler.flush();
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
        actual = wire2.toString().trim();
        if (afterRun != null) {
            expected = afterRun.apply(expected);
            actual = afterRun.apply(actual);
        }
        return this;
    }

    private void consumeDocumentSeperator(@NotNull Wire wireOut) {
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

    static class Invocation {
        Method method;
        Object[] args;

        public Invocation(Method method, Object[] args) {
            this.method = method;
            this.args = args;
        }
    }

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
                for (String s : retainLast) {
                    key.append(",").append(m.getField(s, Object.class));
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
