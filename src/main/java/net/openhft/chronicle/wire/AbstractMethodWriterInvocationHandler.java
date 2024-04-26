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

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.MethodWriterInvocationHandler;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.util.AbstractInvocationHandler;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An abstract handler for method writer invocations that provides a base implementation.
 * It manages the invocation by mapping methods to their respective parameter holders and writing them to wires.
 * This class can handle both regular and generic events and supports method IDs for binary output.
 */
@Deprecated(/* to be moved to services in x.27 */)
public abstract class AbstractMethodWriterInvocationHandler extends AbstractInvocationHandler implements MethodWriterInvocationHandler {

    // Map to cache the parameter holders for method invocations
    private final Map<Method, ParameterHolderSequenceWriter> parameterMap = new ConcurrentHashMap<>();
    protected boolean recordHistory;

    // Name for the generic event, if any. A generic event take the event name as the first argument
    protected String genericEvent = "";

    // Flag to determine if method IDs should be used in binary output
    private boolean useMethodIds;

    /**
     * Constructor accepting the type of class the handler operates on.
     *
     * @param tClass The class type this handler is designed for.
     */
    protected AbstractMethodWriterInvocationHandler(Class<?> tClass) {
        super(tClass);
    }

    @Override
    protected Object doInvoke(Object proxy, Method method, Object[] args) {
        handleInvoke(method, args);

        return method.getReturnType().isInterface() ? proxy : null;
    }

    @Override
    public void genericEvent(String genericEvent) {
        this.genericEvent = genericEvent;
    }

    /**
     * Abstract method to handle a method invocation with its respective arguments.
     *
     * @param method The method being invoked.
     * @param args   Arguments provided for the method invocation.
     */
    protected abstract void handleInvoke(Method method, Object[] args);

    /**
     * Handles the method invocation, writes the event details to the provided wire,
     * and supports optional recording of method history.
     *
     * @param method The method being invoked.
     * @param args   Arguments provided for the method invocation.
     * @param wire   The wire output to write the event details.
     * @throws InvalidMarshallableException If there's an error during marshalling.
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

    // Helper to write a regular event to the wire
    private void writeEvent(Wire wire, @NotNull Method method, String methodName, Object[] args) throws InvalidMarshallableException {
        writeEvent0(wire, method, args, methodName, 0);
    }

    // Helper to write a generic event to the wire
    private void writeGenericEvent(Wire wire, @NotNull Method method, Object[] args) throws InvalidMarshallableException {
        String methodName = args[0].toString();
        writeEvent0(wire, method, args, methodName, 1);
    }

    // Core logic to write events to the wire, distinguishing between methods with and without arguments
    @SuppressWarnings("unchecked")
    private void writeEvent0(Wire wire, @NotNull Method method, Object[] args, String methodName, int oneParam) throws InvalidMarshallableException {
        // Fetch or compute the parameter holder for the method
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

    @Override
    public void recordHistory(boolean recordHistory) {
        this.recordHistory = recordHistory;
    }

    @Override
    public void useMethodIds(boolean useMethodIds) {
        this.useMethodIds = useMethodIds;
    }
}
