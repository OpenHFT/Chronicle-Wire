package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import net.openhft.chronicle.wire.AbstractMarshallableCfg;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class CNFREOnMissingClassTest {
    @Test(expected = ClassNotFoundRuntimeException.class)
    public void throwIllegalArgumentExceptionOnMissingClassAlias() {
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
        ClassAliasPool.CLASS_ALIASES.addAlias(TwoFields.class);
        String simpleObject = "!TwoFields { name: \"henry\", fieldOne: !ThisClassDoesntExist { } }";
        String key = "class.not.found.for.missing.class.alias";
        Jvm.startup().on(CNFREOnMissingClassTest.class, "Value of " + key + ": " + Jvm.getBoolean(key));
        final TwoFields simple = Marshallable.fromString(simpleObject);
    }
}
