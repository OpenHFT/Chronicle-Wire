package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.ObjectUtils;

import java.util.List;
import java.util.function.Function;

// TODO to be removed before merge
// TODO code like this will be generated
public class GeneratedParseFunction implements Function<WireIn, Boolean> {
    // instances
    private Object instance1;
    private Object instance2;

    // system parselets
    private final WireParselet messageHistoryParselet;
    private final WireParselet debugLoggingParselet;

    // method name consumer SB
    private final StringBuilder lastEventName = new StringBuilder(128);

    // MyInterface1
    private String call2arg1;

    private long call3arg1;
    private int call3arg2;

    private Class call4arg1type = ObjectUtils.implementationToUse(byte[].class);
    private byte[] call4arg1;
    private double call4arg2;
    private Class call4arg3type = ObjectUtils.implementationToUse(List.class);
    private List call4arg3;

    private Class call5arg1type = ObjectUtils.implementationToUse(Object.class);
    private Object call5arg1;

    private Class call6arg1type = ObjectUtils.implementationToUse(TestClassesStorage.MyCustomType1.class);
    private TestClassesStorage.MyCustomType1 call6arg1 = ObjectUtils.newInstance(TestClassesStorage.MyCustomType1.class); // Marshallable initialization
    private Class call6arg2type = ObjectUtils.implementationToUse(TestClassesStorage.MyCustomType2.class);
    private TestClassesStorage.MyCustomType2 call6arg2;
    private int call6arg3;

    private Class call8arg1type = ObjectUtils.implementationToUse(TestClassesStorage.MyCustomType1.class);
    private TestClassesStorage.MyCustomType1 call8arg1 = ObjectUtils.newInstance(TestClassesStorage.MyCustomType1.class); // Marshallable initialization

    // MyInterface2
    private boolean call9arg1;
    private Class call9arg2type = ObjectUtils.implementationToUse(TestClassesStorage.MyCustomType2.class);
    private TestClassesStorage.MyCustomType2 call9arg2;

    // DoChain
    private boolean call10arg1;

    // chained calls results
    private TestClassesStorage.DoChain call8res;
    private TestClassesStorage.DoChain2 call10res;

    // Numeric converters (created in case non-binary wire is used)
//    private final MilliTimestampLongConverter call3arg1converter = new MilliTimestampLongConverter();
//    private final Base32IntConverter call3arg2converter = new Base32IntConverter();

    // flag for handling ignoreMethodBasedOnFirstArg
    private boolean ignored;

    public GeneratedParseFunction(Object instance1,
                                  Object instance2,
                                  WireParselet messageHistoryParselet,
                                  WireParselet debugLoggingParselet) {
        this.instance1 = instance1;
        this.instance2 = instance2;
        this.messageHistoryParselet = messageHistoryParselet;
        this.debugLoggingParselet = debugLoggingParselet;
    }

    @Override
    public Boolean apply(WireIn wireIn) {
        String methodName;
        ValueIn valueIn;

        if (wireIn.bytes().peekUnsignedByte() == BinaryWireCode.FIELD_NUMBER) {
            int methodId = (int) wireIn.readEventNumber();
            valueIn = wireIn.getValueIn();

            switch (methodId) {
                case 5:
                    methodName = "call5";
                    break;

                default:
                    return false;
            }
        }
        else {
            lastEventName.setLength(0);

            valueIn = wireIn.readEventName(lastEventName);

            // todo: avoid String object creation ?
            methodName = lastEventName.toString();
        }

        try {
            if (Jvm.isDebug())
                debugLoggingParselet.accept(methodName, valueIn);

            switch (methodName) {
                case MethodReader.HISTORY:
                    messageHistoryParselet.accept(MethodReader.HISTORY, valueIn);
                    break;

                case "call1":
                    valueIn.skipValue();
                    ((TestClassesStorage.MyInterface1)instance1).call1();
                    break;

                case "call2":
                    call2arg1 = valueIn.text();
                    ((TestClassesStorage.MyInterface1)instance1).call2(call2arg1);
                    break;

                case "call3":
                    valueIn.sequence(this, (f, v) -> {
                        f.call3arg1 = v.int64();
                        // f.call3arg1 = call3arg1converter.parse(v.text()); - in case of non-binary wire
                        f.call3arg2 = v.int32();
                        // f.call3arg2 = call3arg2converter.parse(v.text()); - in case of non-binary wire
                    });
                    ((TestClassesStorage.MyInterface1)instance1).call3(call3arg1, call3arg2);
                    break;

                case "call4":
                    valueIn.sequence(this, (f, v) -> {
                        f.call4arg1 = v.object(f.call4arg1, f.call4arg1type);
                        f.call4arg2 = v.float64();
                        f.call4arg3 = v.object(f.call4arg3, f.call4arg3type);
                    });
                    ((TestClassesStorage.MyInterface1)instance1).call4(call4arg1, call4arg2, call4arg3);
                    break;

                case "call5":
                    call5arg1 = valueIn.object(call5arg1, call5arg1type);
                    ((TestClassesStorage.MyInterface1)instance1).call5(call5arg1);
                    break;

                case "call6":
                    valueIn.sequence(this, (f, v) -> {
                        f.call6arg1 = v.object(f.call6arg1, f.call6arg1type);
                        f.call6arg2 = v.object(f.call6arg2, f.call6arg2type);
                        f.call6arg3 = v.int32();
                    });
                    ((TestClassesStorage.MyInterface1)instance1).call6(call6arg1, call6arg2, call6arg3);
                    break;

                case "call7":
                    valueIn.skipValue();
                    ((TestClassesStorage.MyInterface1)instance1).call7();
                    break;

                case "call8":
                    call8arg1 = valueIn.object(call8arg1, call8arg1type);
                    call8res = ((TestClassesStorage.MyInterface1)instance1).call8(call8arg1);
                    break;

                case "call9":
                    ignored = false;

                    valueIn.sequence(this, (f, v) -> {
                        f.call9arg1 = v.bool();

                        if (((MethodFilterOnFirstArg)f.instance2).ignoreMethodBasedOnFirstArg("call9", f.call9arg1)) {
                            f.ignored = true;
                        } else {
                            f.call9arg2 = v.object(f.call9arg2, f.call9arg2type);
                        }
                    });

                    if (!ignored)
                        ((TestClassesStorage.MyInterface2)instance2).call9(call9arg1, call9arg2);

                    break;

                case "call10":
                    call10arg1 = valueIn.bool();
                    call10res = call8res.call10(call10arg1);
                    break;

                case "call12":
                    valueIn.skipValue();
                    call10res.call12();
                    break;

                case "call14":
                    valueIn.skipValue();
                    call8res.call14();
                    break;

                default:
                    return false;
            }

            return true;
        }
        catch (Exception e) {
            Jvm.warn().on(this.getClass(), "Failure to dispatch message, " +
                    "will retry to process without generated code: " + methodName + "()", e);

            return false;
        }
    }
}
