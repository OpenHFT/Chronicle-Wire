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

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.MethodReaderInterceptorReturns;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.InvocationTargetRuntimeException;
import net.openhft.chronicle.wire.*;

/**
 * A generated method reader designed to interpret and process method calls
 * from wire-format messages, particularly focusing on echoing instances of TopOfBook.
 */
public class EchoTopOfBookHandlerMethodReader extends AbstractGeneratedMethodReader {

    // Instance on which parsed calls are invoked
    private final Object instance0;

    // Default parselet used for handling unexpected method calls
    private final WireParselet defaultParselet;

    // Holder for the 'out' method argument (not used in provided code)
    private net.openhft.chronicle.wire.channel.book.TopOfBookListener outarg0;

    // Holder for the 'topOfBook' method argument
    private net.openhft.chronicle.wire.channel.book.TopOfBook topOfBookarg0 = new TopOfBook();

    /**
     * Constructor initializing reader components and setting up parsing infrastructure.
     */
    public EchoTopOfBookHandlerMethodReader(MarshallableIn in, WireParselet defaultParselet, WireParselet debugLoggingParselet, MethodReaderInterceptorReturns interceptor, Object[] metaInstances, Object[] instances) {
        super(in, debugLoggingParselet);
        this.defaultParselet = defaultParselet;
        instance0 = instances[0];
    }

    /**
     * Parses one method call from the provided wire message, interpreting its method identifier
     * and arguments, then dispatching to the corresponding method.
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

    // Local method to read 'topOfBook' data and invoke corresponding method on instance0
    private void readTopOfBook(ValueIn valueIn) {
        // Deserialize the incoming data into a TopOfBook instance, reusing existing instance if possible
        topOfBookarg0 = (TopOfBook) valueIn.marshallable(topOfBookarg0, SerializationStrategies.MARSHALLABLE);
        try {
            dataEventProcessed = true;  // Note: Ensure 'dataEventProcessed' is defined and relevant in your context.
                // Dispatch to the 'topOfBook' method on the target instance, providing the deserialized data
            ((TopOfBookListener) instance0).topOfBook(topOfBookarg0);
        } catch (Exception e) {
            // Wrap and propagate any exceptions encountered during method dispatch
            throw new InvocationTargetRuntimeException(e);
        }
    }

    /**
     * Parses one method call from the provided wire message, interpreting its method identifier
     * and arguments in a context of metadata, then dispatching to the corresponding method
     * or handling as appropriate.
     */
    @Override
    protected boolean readOneCallMeta(WireIn wireIn) {
        ValueIn valueIn = wireIn.getValueIn();
        String lastEventName = "";
        if (wireIn.bytes().peekUnsignedByte() == BinaryWireCode.FIELD_NUMBER) {
            int methodId = (int) wireIn.readEventNumber();
            switch (methodId) {
                default:
                    valueIn.skipValue();
                    return true;
            }
        } else {
            lastEventName = wireIn.readEvent(String.class);
        }
        try {
            if (Jvm.isDebug())
                debugLoggingParselet.accept(lastEventName, valueIn);
            if (lastEventName == null)
                throw new IllegalStateException("Failed to read method name or ID");

            switch (lastEventName) {
                case MethodReader.HISTORY:
                    valueIn.marshallable(messageHistory);
                    break;

                default:
                    valueIn.skipValue();
                    return true;
            }
            return true;
        } catch (InvocationTargetRuntimeException e) {
            throw e;
        }
    }
}
