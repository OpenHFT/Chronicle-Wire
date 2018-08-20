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
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.MethodWriterInterceptor;
import net.openhft.chronicle.bytes.MethodWriterInvocationHandler;
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
    private BiConsumer<Method, Object[]> handleInvoke;
    // Note the Object[] passed in creates an object on every call.
    private boolean useMethodIds;

    public AbstractMethodWriterInvocationHandler() {
        super(HashMap::new);
        this.handleInvoke = this::handleInvoke;
    }

    @Override
    protected Object doInvoke(Object proxy, Method method, Object[] args) {
        if (methodWriterInterceptor != null)
            methodWriterInterceptor.intercept(method, args, this.handleInvoke);
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

    private void writeEvent0(Wire wire, @NotNull Method method, Object[] args, String methodName, int oneParam) {
        ParameterHolderSequenceWriter phsw = parameterMap.computeIfAbsent(method, ParameterHolderSequenceWriter::new);
        Bytes<?> bytes = wire.bytes();
        if (bytes.retainsComments())
            bytes.comment(methodName);
        ValueOut valueOut = useMethodIds && phsw.methodId >= 0
                ? wire.writeEventId((int) phsw.methodId)
                : wire.writeEventName(methodName);
        switch (args.length - oneParam) {
            case 0:
                valueOut.text("");
                break;
            case 1:
                Object arg = args[oneParam];
                if (bytes.retainsComments())
                    addComment(bytes, arg);
                if (arg != null && arg.getClass() == RawText.class)
                    valueOut.rawText(((RawText) arg).text);
                else
                    valueOut.object(phsw.parameterTypes[oneParam], arg);
                break;
            default:
                valueOut.sequence(args, oneParam == 0 ? phsw.from0 : phsw.from1);
        }
    }

    private void addComment(Bytes<?> bytes, Object arg) {
        if (arg instanceof Marshallable)
            bytes.comment(arg.getClass().getSimpleName());
        else
            bytes.comment(String.valueOf(arg));
    }

    @Override
    public void recordHistory(boolean recordHistory) {
        this.recordHistory = recordHistory;
    }

    @Override
    public void methodWriterInterceptor(MethodWriterInterceptor methodWriterInterceptor) {
        this.methodWriterInterceptor = methodWriterInterceptor;
    }

    @Override
    public void useMethodIds(boolean useMethodIds) {
        this.useMethodIds = useMethodIds;
    }
}
