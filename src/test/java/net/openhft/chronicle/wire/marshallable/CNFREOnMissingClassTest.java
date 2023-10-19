package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import net.openhft.chronicle.wire.*;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class CNFREOnMissingClassTest extends WireTestCommon {
    @Test(expected = ClassNotFoundRuntimeException.class)
    public void throwIllegalArgumentExceptionOnMissingClassAlias() {
        WiresTest.wiresThrowCNFRE(true);
        Wire wire = new TextWire(Bytes.from("" +
                "a: !Aaa { hi: bye }"));
        Object object = wire.read("a").object();
        System.out.println(object);
        assertNotNull(object);
    }

    static class TwoFields extends AbstractMarshallableCfg {
        private String name;
        private Object fieldOne;
    }

    @Test(expected = ClassNotFoundRuntimeException.class)
    public void throwClassNotFoundRuntimeExceptionOnMissingClassForField() {
        WiresTest.wiresThrowCNFRE(true);
        ClassAliasPool.CLASS_ALIASES.addAlias(TwoFields.class);
        String simpleObject = "!TwoFields { name: \"henry\", fieldOne: !ThisClassDoesntExist { } }";
        String key = "class.not.found.for.missing.class.alias";
        Jvm.startup().on(CNFREOnMissingClassTest.class, "Value of " + key + ": " + Jvm.getBoolean(key));
        final TwoFields simple = Marshallable.fromString(simpleObject);
    }

    static class UsesTwoFields extends AbstractMarshallableCfg {
        private TwoFields bothFields;
        private String name;
    }

    /**
     * Failing to load a class for a field with a type of java.lang.Object causes the correct behaviour but in
     * an unexpected code path (the check for a classloader at TextWire#typeOrPrefixObject - line 1913
     */
    @Test(expected = ClassNotFoundRuntimeException.class)
    public void throwClassNotFoundRuntimeExceptionOnMissingClassForFieldNotObject() {
        WiresTest.wiresThrowCNFRE(true);
        ClassAliasPool.CLASS_ALIASES.addAlias(TwoFields.class, UsesTwoFields.class);
        String key = "class.not.found.for.missing.class.alias";
        String simpleObject = "!UsesTwoFields { name: \"henry\", bothFields: !ThisClassDoesntExist { } }";
        Jvm.startup().on(CNFREOnMissingClassTest.class, "Value of " + key + ": " + Jvm.getBoolean(key));
        final UsesTwoFields simple = Marshallable.fromString(simpleObject);
    }

}
