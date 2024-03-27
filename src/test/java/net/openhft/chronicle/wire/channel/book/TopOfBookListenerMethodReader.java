/*
 * Copyright 2016-2022 chronicle.software
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

package net.openhft.chronicle.wire.channel.book;

// Generated code added here as there is an issue with Java 18.

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.MethodReaderInterceptorReturns;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.InvocationTargetRuntimeException;
import net.openhft.chronicle.wire.*;

/**
 * A class for reading methods pertaining to the top of the book
 * in financial markets, extending an abstract method reader for generated methods.
 */
public class TopOfBookListenerMethodReader extends AbstractGeneratedMethodReader {

    // Instance of the object upon which parsed calls are invoked
    private final Object instance0;

    // Default parselet used to define default behaviors or fallbacks during parsing
    private final WireParselet defaultParselet;

    // Object representing the top of the book in financial market context
    private net.openhft.chronicle.wire.channel.book.TopOfBook topOfBookarg0 = new TopOfBook();

    /**
     * Constructor for initializing the method reader with necessary context and instances.
     *
     * @param in                   Input object for marshallable data.
     * @param defaultParselet      The default parselet to handle unspecified or generic parsing tasks.
     * @param debugLoggingParselet Parselet used for debugging and logging.
     * @param interceptor          Interceptor for method reader returns.
     * @param metaInstances        Array of instances carrying metadata for method reading.
     * @param instances            Array of instances upon which method reading would be performed.
     */
    public TopOfBookListenerMethodReader(MarshallableIn in, WireParselet defaultParselet, WireParselet debugLoggingParselet, MethodReaderInterceptorReturns interceptor, Object[] metaInstances, Object[] instances) {
        super(in, debugLoggingParselet);
        this.defaultParselet = defaultParselet;
        instance0 = instances[0];
    }

    /**
     * Reads a single call from the wire input, parsing data and handling events.
     *
     * @param wireIn The input wire from which data is read.
     * @return True if the read is successful; otherwise, false.
     */
    @Override
    protected boolean readOneCall(WireIn wireIn) {
        ValueIn valueIn = wireIn.getValueIn();
        String lastEventName = "";
        final long methodId0 = wireIn.readEventNumber();
        if (methodId0 != Long.MIN_VALUE) {
            int methodId = (int) methodId0;
            switch (methodId) {
                case 116:
                    readTopOfBook(valueIn);
                    return true;

                case -1:
                    lastEventName = "history";
                    break;

                default:
                    lastEventName = Integer.toString(methodId);
                    break;
            }
        } else {
            lastEventName = wireIn.readEvent(String.class);
        }
        try {
            if (Jvm.isDebug())
                debugLoggingParselet.accept(lastEventName, valueIn);
            if (lastEventName == null)
                throw new IllegalStateException("Failed to read method name or ID");
            // Reading topOfBook event and invoking associated method
            switch (lastEventName) {
                case MethodReader.HISTORY:
                    valueIn.marshallable(messageHistory);
                    break;

                case "topOfBook":
                    readTopOfBook(valueIn);
                    break;

                default:
                    defaultParselet.accept(lastEventName, valueIn);
                    return true;
            }
            return true;
        } catch (InvocationTargetRuntimeException e) {
            throw e;
        }
    }

    /**
     * Reads the 'topOfBook' data from the wire input and invokes the method on the instance.
     *
     * @param valueIn Input value from the wire to be parsed and processed.
     */
    private void readTopOfBook(ValueIn valueIn) {
        topOfBookarg0 = (TopOfBook) valueIn.marshallable(topOfBookarg0, SerializationStrategies.MARSHALLABLE);
        // Catching and handling exceptions during method invocation
        try {
            // Setting flag and invoking 'topOfBook' method on the listener instance
            dataEventProcessed = true;
            ((TopOfBookListener) instance0).topOfBook(topOfBookarg0);
        } catch (Exception e) {
            throw new InvocationTargetRuntimeException(e);
        }
    }

    /**
     * Reads a single meta call from the wire input, parsing and handling metadata events.
     *
     * @param wireIn The input wire from which metadata is read.
     * @return True if the read is successful; otherwise, false.
     */
    @Override
    protected boolean readOneCallMeta(WireIn wireIn) {
        ValueIn valueIn = wireIn.getValueIn();
        String lastEventName = "";
        if (wireIn.bytes().peekUnsignedByte() == BinaryWireCode.FIELD_NUMBER) {
            int methodId = (int) wireIn.readEventNumber();
            valueIn.skipValue();
            return true;
        } else {
            lastEventName = wireIn.readEvent(String.class);
        }
        // Exception handling and debugging/logging
        try {
            if (Jvm.isDebug())
                debugLoggingParselet.accept(lastEventName, valueIn);
            if (lastEventName == null)
                throw new IllegalStateException("Failed to read method name or ID");
            if (MethodReader.HISTORY.equals(lastEventName)) {
                valueIn.marshallable(messageHistory);
            } else {
                valueIn.skipValue();
                return true;
            }
            return true;
        } catch (InvocationTargetRuntimeException e) {
            throw e;
        }
    }
}
