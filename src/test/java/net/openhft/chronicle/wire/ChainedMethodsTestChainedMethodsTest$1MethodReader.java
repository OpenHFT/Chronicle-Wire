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

package net.openhft.chronicle.wire;

// NOTE: Added to check backward compatibility

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.MethodReaderInterceptorReturns;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.InvocationTargetRuntimeException;

public class ChainedMethodsTestChainedMethodsTest$1MethodReader extends AbstractGeneratedMethodReader {
    // The object instance on which the parsed method calls are to be invoked
    private final Object instance0;
    // Default parselet used for reading method invocations
    private final WireParselet defaultParselet;

    // mid2
    private String mid2arg0;

    // dto
    private DMOuterClass dtoarg0;

    // next2
    private String next2arg0;

    // echo
    private String echoarg0;

    // midTwoArgs
    private int midTwoArgsarg0;
    private long midTwoArgsarg1;

    // next
    private long nextarg0;

    // mid
    private String midarg0;

    // Holds the result of a chained call, if any
    private Object chainedCallReturnResult;

    public ChainedMethodsTestChainedMethodsTest$1MethodReader(MarshallableIn in, WireParselet defaultParselet, WireParselet debugLoggingParselet, MethodReaderInterceptorReturns interceptor, Object[] metaInstances, Object[] instances) {
        super(in, debugLoggingParselet);
        this.defaultParselet = defaultParselet;
        instance0 = instances[0];
    }

    // Checks if the result of the chained call is an instance of the IgnoresEverything interface
    @Override
    public boolean restIgnored() {
        return chainedCallReturnResult instanceof net.openhft.chronicle.core.util.IgnoresEverything;
    }

    // Attempts to read a single method call from the input and invoke the corresponding method on an instance
    @Override
    protected boolean readOneCall(WireIn wireIn) {
        ValueIn valueIn = wireIn.getValueIn();
        String lastEventName = "";

        // Check the type of method identification (by name or ID)
        if (wireIn.bytes().peekUnsignedByte() == BinaryWireCode.FIELD_NUMBER) {
            int methodId = (int) wireIn.readEventNumber();
            switch (methodId) {
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

        // Handle the identified method
        try {
            // If in debug mode, log the method being invoked
            if (Jvm.isDebug())
                debugLoggingParselet.accept(lastEventName, valueIn);

            // If method name or ID isn't available, throw an error
            if (lastEventName == null)
                throw new IllegalStateException("Failed to read method name or ID");

            // Switch on the method's name to determine which method to invoke
            switch (lastEventName) {
                case MethodReader.HISTORY:
                    valueIn.marshallable(messageHistory);
                    break;

                case "mid2":
                    mid2arg0 = valueIn.text();
                    try {
                        dataEventProcessed = true;
                        chainedCallReturnResult = ((ITop) instance0).mid2(mid2arg0);
                    } catch (Exception e) {
                        throw new InvocationTargetRuntimeException(e);
                    }
                    break;

                case "dto":
                    dtoarg0 = valueIn.object(checkRecycle(dtoarg0), DMOuterClass.class);
                    try {
                        dataEventProcessed = true;
                        ((IMid2) chainedCallReturnResult).dto(dtoarg0);
                    } catch (Exception e) {
                        throw new InvocationTargetRuntimeException(e);
                    }
                    break;

                // Handle the "next2" method call: Extract the argument and invoke the corresponding method on IMid2 interface
                case "next2":
                    next2arg0 = valueIn.text();
                    try {
                        dataEventProcessed = true;
                        chainedCallReturnResult = ((IMid2) chainedCallReturnResult).next2(next2arg0);
                    } catch (Exception e) {
                        throw new InvocationTargetRuntimeException(e);
                    }
                    break;

                // Handle the "echo" method call: Extract the argument and invoke the corresponding method on ILast interface
                case "echo":
                    echoarg0 = valueIn.text();
                    try {
                        dataEventProcessed = true;
                        ((ILast) chainedCallReturnResult).echo(echoarg0);
                    } catch (Exception e) {
                        throw new InvocationTargetRuntimeException(e);
                    }
                    break;

                // Handle the "midTwoArgs" method call: Extract the arguments using sequence and invoke the corresponding method on ITop interface
                case "midTwoArgs":
                    valueIn.sequence(this, (f, v) -> { // todo optimize megamorphic lambda call
                        f.midTwoArgsarg0 = v.int32();
                        f.midTwoArgsarg1 = v.int64();
                    });
                    try {
                        dataEventProcessed = true;
                        chainedCallReturnResult = ((ITop) instance0).midTwoArgs(midTwoArgsarg0, midTwoArgsarg1);
                    } catch (Exception e) {
                        throw new InvocationTargetRuntimeException(e);
                    }
                    break;

                // Handle the "next" method call: Extract the argument and invoke the corresponding method on IMid interface
                case "next":
                    nextarg0 = valueIn.int64();
                    try {
                        dataEventProcessed = true;
                        chainedCallReturnResult = ((IMid) chainedCallReturnResult).next(nextarg0);
                    } catch (Exception e) {
                        throw new InvocationTargetRuntimeException(e);
                    }
                    break;

                // Handle the "midNoArg" method call: Skip reading the value and invoke the corresponding method on ITop interface
                case "midNoArg":
                    valueIn.skipValue();
                    try {
                        dataEventProcessed = true;
                        chainedCallReturnResult = ((ITop) instance0).midNoArg();
                    } catch (Exception e) {
                        throw new InvocationTargetRuntimeException(e);
                    }
                    break;

                // Handle the "mid" method call: Extract the argument and invoke the corresponding method on ITop interface
                case "mid":
                    midarg0 = valueIn.text();
                    try {
                        dataEventProcessed = true;
                        chainedCallReturnResult = ((ITop) instance0).mid(midarg0);
                    } catch (Exception e) {
                        throw new InvocationTargetRuntimeException(e);
                    }
                    break;

                // Handle any method calls not explicitly accounted for
                default:
                    defaultParselet.accept(lastEventName, valueIn);
            }
            return true;
        } catch (InvocationTargetRuntimeException e) {
            throw e;
        }
    }

    // Read and process metadata associated with the call
    @Override
    protected boolean readOneCallMeta(WireIn wireIn) {
        ValueIn valueIn = wireIn.getValueIn();
        String lastEventName = "";

        // Determine whether the event is indicated by a FIELD_NUMBER or a String
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

        // Handle debug logging and read the method name or ID
        try {
            if (Jvm.isDebug())
                debugLoggingParselet.accept(lastEventName, valueIn);
            if (lastEventName == null)
                throw new IllegalStateException("Failed to read method name or ID");

            // Handle specific event names or skip reading the value
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
