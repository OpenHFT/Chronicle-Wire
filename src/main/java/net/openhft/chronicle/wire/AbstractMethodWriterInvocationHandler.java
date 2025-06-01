/*
 * Copyright 2016-2025 chronicle.software
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

import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.MethodWriterInvocationHandler;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.util.AbstractInvocationHandler;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base for {@link MethodWriterInvocationHandler}s. It intercepts method
 * calls on a proxy and serialises them to a {@link Wire}. Parameter writers
 * ({@link ParameterHolderSequenceWriter}) are cached for reuse. Subclasses
 * implement {@link #handleInvoke(Method, Object[])} to obtain the target wire
 * and perform the actual write.
 */
public abstract class AbstractMethodWriterInvocationHandler extends AbstractInvocationHandler implements MethodWriterInvocationHandler {

    /** Cache of analysed parameter writers keyed by method. */
    private final Map<Method, ParameterHolderSequenceWriter> parameterMap = new ConcurrentHashMap<>();

    /** If true a {@link MessageHistory} is written before each event. */
    protected boolean recordHistory;

    /**
     * Name of the method used for generic events. The first argument becomes
     * the event name on the wire.
     */
    protected String genericEvent = "";

    /** Use numeric {@link MethodId} values when writing to binary wires. */
    private boolean useMethodIds;

    /**
     * @param tClass the primary interface handled by this instance
     */
    protected AbstractMethodWriterInvocationHandler(Class<?> tClass) {
        super(tClass);
    }

    /**
     * Invoked by the proxy for every method call. Delegates to
     * {@link #handleInvoke(Method, Object[])} and returns the proxy when the
     * method itself returns an interface, enabling chaining.
     */
    @Override
    protected Object doInvoke(Object proxy, Method method, Object[] args) {
        handleInvoke(method, args);

        return method.getReturnType().isInterface() ? proxy : null;
    }

    /**
     * Configures which method acts as the generic event dispatcher.
     */
    @Override
    public void genericEvent(String genericEvent) {
        this.genericEvent = genericEvent;
    }

    /**
     * Obtains a {@link WireOut} or {@link DocumentContext} and delegates to
     * {@link #handleInvoke(Method, Object[], Wire)}.
     */
    protected abstract void handleInvoke(Method method, Object[] args);

    /**
     * Core logic for writing a method call to the supplied wire. Writes
     * {@link MessageHistory} when {@link #recordHistory} is true and then
     * serialises either a generic or regular event.
     *
     * @param method the method being invoked
     * @param args   arguments for the invocation
     * @param wire   target wire
     * @throws InvalidMarshallableException if an argument cannot be written
     */
    protected void handleInvoke(@NotNull Method method, Object[] args, Wire wire) throws InvalidMarshallableException {
        if (recordHistory) {
            wire.writeEventName(MethodReader.HISTORY)
                    .marshallable(MessageHistory.get());
        }
        String methodName = method.getName();

        // Distinguish between a generic event and a regular one
        if (methodName.equals(genericEvent)) {
            writeGenericEvent(wire, method, args);
            return;
        }
        writeEvent(wire, method, methodName, args);
    }

    /**
     * Serialises a regular method invocation.
     */
    private void writeEvent(Wire wire, @NotNull Method method, String methodName, Object[] args) throws InvalidMarshallableException {
        writeEvent0(wire, method, args, methodName, 0);
    }

    /**
     * Handles the generic event case where the first argument supplies the event name.
     */
    private void writeGenericEvent(Wire wire, @NotNull Method method, Object[] args) throws InvalidMarshallableException {
        String methodName = args[0].toString();
        writeEvent0(wire, method, args, methodName, 1);
    }

    /**
     * Writes the event name or ID followed by the parameters.
     *
     * @param wire      target wire
     * @param method    source method
     * @param args      arguments array
     * @param methodName resolved name to write
     * @param oneParam   index offset when the first argument is the event name
     */
    @SuppressWarnings("unchecked")
    private void writeEvent0(Wire wire, @NotNull Method method, Object[] args, String methodName, int oneParam) throws InvalidMarshallableException {
        final ParameterHolderSequenceWriter phsw = parameterMap.computeIfAbsent(method, ParameterHolderSequenceWriter::new);
        boolean useMethodId = useMethodIds && phsw.methodId >= 0 && wire.getValueOut().isBinary();
        ValueOut valueOut = useMethodId
                ? wire.writeEventId((int) phsw.methodId)
                : wire.writeEventName(methodName);

        // Write to the wire based on argument count
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

    /**
     * Enables or disables writing of {@link MessageHistory} before each event.
     */
    @Override
    public void recordHistory(boolean recordHistory) {
        this.recordHistory = recordHistory;
    }

    /**
     * Controls whether numeric {@link MethodId} values are emitted rather than names.
     */
    @Override
    public void useMethodIds(boolean useMethodIds) {
        this.useMethodIds = useMethodIds;
    }
}
