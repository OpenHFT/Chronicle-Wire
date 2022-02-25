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

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.MethodWriterInterceptorReturns;
import net.openhft.chronicle.bytes.MethodWriterInvocationHandler;
import net.openhft.chronicle.core.util.AbstractInvocationHandler;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public abstract class AbstractMethodWriterInvocationHandler extends AbstractInvocationHandler implements MethodWriterInvocationHandler {
    private final Map<Method, ParameterHolderSequenceWriter> parameterMap = new ConcurrentHashMap<>();
    private final ThreadLocal<Object> proxy = new ThreadLocal<>();
    private final BiFunction<Method, Object[], Object> onMethod = (m, a) -> {
        this.handleInvoke.accept(m, a);
        return m.getReturnType().isInterface() ? this.proxy.get() : null;
    };
    protected boolean recordHistory;
    protected String genericEvent = "";
    private MethodWriterInterceptorReturns methodWriterInterceptorReturns;
    private BiConsumer<Method, Object[]> handleInvoke;
    private boolean useMethodIds;

    protected AbstractMethodWriterInvocationHandler(Class tClass) {
        super(tClass);
        this.handleInvoke = this::handleInvoke;
    }

    @Override
    protected Object doInvoke(Object proxy, Method method, Object[] args) {

        if (methodWriterInterceptorReturns != null) {
            this.proxy.set(proxy);
            // TODO: ignores retval
            methodWriterInterceptorReturns.intercept(method, args, onMethod);
        } else {
            handleInvoke(method, args);
        }
        return method.getReturnType().isInterface() ? proxy : null;
    }

    @Override
    public void genericEvent(String genericEvent) {
        this.genericEvent = genericEvent;
    }

    protected abstract void handleInvoke(Method method, Object[] args);

    protected void handleInvoke(@NotNull Method method, Object[] args, Wire wire) {
        if (recordHistory) {
            wire.writeEventName(MethodReader.HISTORY)
                    .marshallable(MessageHistory.get());
        }
        String methodName = method.getName();
        if (methodName.equals(genericEvent)) {
            writeGenericEvent(wire, method, args);
            return;
        }
        writeEvent(wire, method, methodName, args);
    }

    private void writeEvent(Wire wire, @NotNull Method method, String methodName, Object[] args) {
        writeEvent0(wire, method, args, methodName, 0);
    }

    private void writeGenericEvent(Wire wire, @NotNull Method method, Object[] args) {
        String methodName = args[0].toString();
        writeEvent0(wire, method, args, methodName, 1);
    }

    @SuppressWarnings("unchecked")
    private void writeEvent0(Wire wire, @NotNull Method method, Object[] args, String methodName, int oneParam) {
        final ParameterHolderSequenceWriter phsw = parameterMap.computeIfAbsent(method, ParameterHolderSequenceWriter::new);
        boolean useMethodId = useMethodIds && phsw.methodId >= 0 && wire.getValueOut().isBinary();
        ValueOut valueOut = useMethodId
                ? wire.writeEventId((int) phsw.methodId)
                : wire.writeEventName(methodName);
        switch (args.length - oneParam) {
            case 0:
                valueOut.text("");
                break;
            case 1:
                Object arg = args[oneParam];
                if (arg != null && arg.getClass() == RawText.class)
                    valueOut.rawText(((RawText) arg).text);
                else
                    valueOut.object(phsw.parameterTypes[oneParam], arg);
                break;
            default:
                valueOut.sequence(args, oneParam == 0 ? phsw.from0 : phsw.from1);
        }
    }

    @Override
    public void recordHistory(boolean recordHistory) {
        this.recordHistory = recordHistory;
    }

    @Override
    public void methodWriterInterceptorReturns(MethodWriterInterceptorReturns methodWriterInterceptorReturns) {
        this.methodWriterInterceptorReturns = methodWriterInterceptorReturns;
    }

    @Override
    public void useMethodIds(boolean useMethodIds) {
        this.useMethodIds = useMethodIds;
    }
}
