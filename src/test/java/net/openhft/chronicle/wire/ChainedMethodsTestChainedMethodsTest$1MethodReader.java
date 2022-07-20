package net.openhft.chronicle.wire;

// NOTE: Added to check backward compatibility

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.MethodReaderInterceptorReturns;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.InvocationTargetRuntimeException;

public class ChainedMethodsTestChainedMethodsTest$1MethodReader extends AbstractGeneratedMethodReader {
    // instances on which parsed calls are invoked
    private final Object instance0;
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

    // chained call result
    private Object chainedCallReturnResult;

    public ChainedMethodsTestChainedMethodsTest$1MethodReader(MarshallableIn in, WireParselet defaultParselet, WireParselet debugLoggingParselet, MethodReaderInterceptorReturns interceptor, Object[] metaInstances, Object[] instances) {
        super(in, debugLoggingParselet);
        this.defaultParselet = defaultParselet;
        instance0 = instances[0];
    }

    @Override
    public boolean restIgnored() {
        return chainedCallReturnResult instanceof net.openhft.chronicle.core.util.IgnoresEverything;
    }

    @Override
    protected boolean readOneCall(WireIn wireIn) {
        ValueIn valueIn = wireIn.getValueIn();
        String lastEventName = "";
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
        try {
            if (Jvm.isDebug())
                debugLoggingParselet.accept(lastEventName, valueIn);
            if (lastEventName == null)
                throw new IllegalStateException("Failed to read method name or ID");
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

                case "next2":
                    next2arg0 = valueIn.text();
                    try {
                        dataEventProcessed = true;
                        chainedCallReturnResult = ((IMid2) chainedCallReturnResult).next2(next2arg0);
                    } catch (Exception e) {
                        throw new InvocationTargetRuntimeException(e);
                    }
                    break;

                case "echo":
                    echoarg0 = valueIn.text();
                    try {
                        dataEventProcessed = true;
                        ((ILast) chainedCallReturnResult).echo(echoarg0);
                    } catch (Exception e) {
                        throw new InvocationTargetRuntimeException(e);
                    }
                    break;

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

                case "next":
                    nextarg0 = valueIn.int64();
                    try {
                        dataEventProcessed = true;
                        chainedCallReturnResult = ((IMid) chainedCallReturnResult).next(nextarg0);
                    } catch (Exception e) {
                        throw new InvocationTargetRuntimeException(e);
                    }
                    break;

                case "midNoArg":
                    valueIn.skipValue();
                    try {
                        dataEventProcessed = true;
                        chainedCallReturnResult = ((ITop) instance0).midNoArg();
                    } catch (Exception e) {
                        throw new InvocationTargetRuntimeException(e);
                    }
                    break;

                case "mid":
                    midarg0 = valueIn.text();
                    try {
                        dataEventProcessed = true;
                        chainedCallReturnResult = ((ITop) instance0).mid(midarg0);
                    } catch (Exception e) {
                        throw new InvocationTargetRuntimeException(e);
                    }
                    break;

                default:
                    defaultParselet.accept(lastEventName, valueIn);
            }
            return true;
        } catch (InvocationTargetRuntimeException e) {
            throw e;
        }
    }

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
