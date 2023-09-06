package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.AbstractMarshallableCfg;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class IAEOnMissingClassTest {
    @Test(expected = IllegalArgumentException.class)
    public void throwIllegalArgumentExceptionOnMissingClassAlias() {
        Wire wire = new TextWire(Bytes.from("" +
                "a: !Aaa { hi: bye }"));
        Object object = wire.read("a").object();
        System.out.println(object);
        assertNotNull(object);
    }

    class TwoFields extends AbstractMarshallableCfg {

        private String name;
        private Object fieldOne;
        private Object fieldTwo;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Object getFieldOne() {
            return fieldOne;
        }

        public void setFieldOne(Object fieldOne) {
            this.fieldOne = fieldOne;
        }

        public Object getFieldTwo() {
            return fieldTwo;
        }

        public void setFieldTwo(Object fieldTwo) {
            this.fieldTwo = fieldTwo;
        }
    }

    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(TwoFields.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwIllegalArgumentExceptionOnMissingClassForField() {
        String simpleObject = "!TwoFields { name: \"henry\", fieldOne: !ThisClassDoesntExist }";
        Jvm.startup().on(IAEOnMissingClassTest.class, "Value of illegal.argument.for.missing.class.alias: " + Jvm.getBoolean("illegal.argument.for.missing.class.alias"));
        final TwoFields simple = Marshallable.fromString(simpleObject);
    }

}
