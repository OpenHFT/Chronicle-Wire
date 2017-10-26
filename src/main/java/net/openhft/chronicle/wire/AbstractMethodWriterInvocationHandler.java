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

import net.openhft.chronicle.core.util.AbstractInvocationHandler;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/*
 * Created by Peter Lawrey on 25/03/16.
 */
public abstract class AbstractMethodWriterInvocationHandler extends AbstractInvocationHandler implements MethodWriterInvocationHandler {
    private final Map<Method, ParameterHolderSequenceWriter> parameterMap = new ConcurrentHashMap<>();

    protected boolean recordHistory;
    protected String genericEvent = "";
    private MethodWriterInterceptor methodWriterInterceptor;
    // Note the Object[] passed in creates an object on every call.


    public AbstractMethodWriterInvocationHandler() {
        super(HashMap::new);
    }

    @Override
    protected Object doInvoke(Object proxy, Method method, Object[] args) {
        if (methodWriterInterceptor != null)
            methodWriterInterceptor.intercept(method, args, this::handleInvoke);
        else
            handleInvoke(method, args);
        return ObjectUtils.defaultValue(method.getReturnType());
    }

    @Override
    public void genericEvent(String genericEvent) {
        this.genericEvent = genericEvent;
    }

    protected abstract void handleInvoke(Method method, Object[] args);

    protected void handleInvoke(@NotNull Method method, Object[] args, Wire wire) {
        if (recordHistory) {
            wire.write(MethodReader.HISTORY)
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
        ValueOut valueOut = wire.writeEventName(methodName);
        ParameterHolderSequenceWriter phsw = parameterMap.computeIfAbsent(method, ParameterHolderSequenceWriter::new);
        switch (args.length) {
            case 0:
                valueOut.text("");
                break;
            case 1:
                valueOut.object(phsw.parameterTypes[0], args[0]);
                break;
            default:
                valueOut.sequence(args, phsw.from0);
        }
    }

    private void writeGenericEvent(Wire wire, @NotNull Method method, Object[] args) {
        ValueOut valueOut = wire.writeEventName(args[0].toString());
        ParameterHolderSequenceWriter phsw = parameterMap.computeIfAbsent(method, ParameterHolderSequenceWriter::new);
        switch (args.length) {
            case 1:
                valueOut.text("");
                break;
            case 2:
                valueOut.object(phsw.parameterTypes[1], args[1]);
                break;
            default:
                valueOut.sequence(args, phsw.from1);
        }
    }

    @Override
    public void recordHistory(boolean recordHistory) {
        this.recordHistory = recordHistory;
    }

    @Override
    public void methodWriterInterceptor(MethodWriterInterceptor methodWriterInterceptor) {
        this.methodWriterInterceptor = methodWriterInterceptor;
    }

    // lambda was causing garbage
    private static class ParameterHolderSequenceWriter {
        final Class[] parameterTypes;
        final BiConsumer<Object[], ValueOut> from0;
        final BiConsumer<Object[], ValueOut> from1;

        private ParameterHolderSequenceWriter(Method method) {
            this.parameterTypes = method.getParameterTypes();
            this.from0 = (a, v) -> {
                for (int i = 0; i < parameterTypes.length; i++)
                    v.object(parameterTypes[i], a[i]);
            };
            this.from1 = (a, v) -> {
                for (int i = 1; i < parameterTypes.length; i++)
                    v.object(parameterTypes[i], a[i]);
            };
        }
    }
}
