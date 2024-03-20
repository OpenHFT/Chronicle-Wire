package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import net.openhft.chronicle.wire.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CNFREOnMissingClassTest extends WireTestCommon {

    /**
     * Validates that a ClassNotFoundRuntimeException is correctly thrown when attempting
     * to deserialize data with a class alias that is not registered in the system.
     */
    @Test(expected = ClassNotFoundRuntimeException.class)
    public void throwClassNotFoundRuntimeExceptionOnMissingClassAlias() {
        WiresTest.wiresThrowCNFRE(true);
        Wires.GENERATE_TUPLES = false;
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

    /**
     * Tests if a ClassNotFoundRuntimeException is thrown when a class for a field is missing.
     */
    @Test(expected = ClassNotFoundRuntimeException.class)
    public void throwClassNotFoundRuntimeExceptionOnMissingClassForField() {
        WiresTest.wiresThrowCNFRE(true);
        Wires.GENERATE_TUPLES = false;
        ClassAliasPool.CLASS_ALIASES.addAlias(TwoFields.class);
        String simpleObject = "!TwoFields { name: \"henry\", fieldOne: !ThisClassDoesntExist { value: 1234 } }";
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
    public void throwClassNotFoundRuntimeExceptionOnMissingClassForField2() {
        testFieldNotObject0(false, true, null);
    }

    @Test(expected = ClassNotFoundRuntimeException.class)
    public void throwClassNotFoundRuntimeExceptionOnMissingClassForFieldNotATuple() {
        testFieldNotObject0(true, true, null);
    }

    @Test
    public void useBaseClassForMissingTypeWithoutTuples() {
        expectException("Cannot find a class for ThisClassDoesntExist are you missing an alias?");
        testFieldNotObject0(false, false, "" +
                "!UsesTwoFields {\n" +
                "  bothFields: {\n" +
                "    name: Jerry\n" +
                "  },\n" +
                "  name: henry\n" +
                "}\n");
    }

    @Test
    public void useBaseClassForMissingTypeWithTuples() {
        // only once
        ignoreException("Cannot find a class for ThisClassDoesntExist are you missing an alias?");
        testFieldNotObject0(true, false, "" +
                "!UsesTwoFields {\n" +
                "  bothFields: {\n" +
                "    name: Jerry\n" +
                "  },\n" +
                "  name: henry\n" +
                "}\n");
    }

    private void testFieldNotObject0(boolean generateTuples, boolean throwCNFE, String expected) {
        Wires.GENERATE_TUPLES = generateTuples;
        WiresTest.wiresThrowCNFRE(throwCNFE);
        ClassAliasPool.CLASS_ALIASES.addAlias(UsesTwoFields.class);
        String simpleObject = "!UsesTwoFields { name: \"henry\", bothFields: !ThisClassDoesntExist { name: Jerry } }";
        final UsesTwoFields simple = Marshallable.fromString(simpleObject);
        assertEquals(expected, simple.toString());
    }


    static class UsesInterfaceField extends AbstractMarshallableCfg {
        private TwoFields bothFields;
        private String name;

        private TestEngineListener engineListener;

        public interface TestEngineListener { }
    }

    /**
     * Tests if a ClassNotFoundRuntimeException is thrown for a missing class for an interface field.
     */
    @Test(expected = ClassNotFoundRuntimeException.class)
    public void throwClassNotFoundRuntimeExceptionOnMissingClassForInterfaceField() {
        testInterfaceFieldTest0(false, true, null);
    }

    /**
     * Tuple generation for an interface field
     * when its corresponding class is not found.
     */
    @Test
    public void useTupleOnMissingClassForInterfaceField() {
        testInterfaceFieldTest0(true, true, "" +
                "!UsesInterfaceField {\n" +
                "  name: henry,\n" +
                "  engineListener: !ThisListenerClassDoesntExist {\n" +
                "    value: 128\n" +
                "  }\n" +
                "}\n");
    }

    /**
     * Tests if ClassNotFoundRuntimeException is suppressed for an interface field with no fallback,
     * when the class for the interface is missing, but Wires CNFE property is set to false
     */
    @Test
    public void throwClassNotFoundRuntimeExceptionOnMissingClassForInterfaceFieldNoFallback() {
        testInterfaceFieldTest0(false, false, null);
    }


    @Test
    public void useTupleOnMissingClassForInterfaceField2() {
        testInterfaceFieldTest0(true, false, "" +
                "!UsesInterfaceField {\n" +
                "  name: henry,\n" +
                "  engineListener: !ThisListenerClassDoesntExist {\n" +
                "    value: 128\n" +
                "  }\n" +
                "}\n");
    }

    private void testInterfaceFieldTest0(boolean generateTuples, boolean throwCNFE, String expected) {
        Wires.GENERATE_TUPLES = generateTuples;
        WiresTest.wiresThrowCNFRE(throwCNFE);

        ClassAliasPool.CLASS_ALIASES.addAlias(UsesInterfaceField.class);
        String simpleObject = "!UsesInterfaceField { name: \"henry\", engineListener: !ThisListenerClassDoesntExist { value: 128 } }";
        final UsesInterfaceField simple = Marshallable.fromString(simpleObject);
        assertEquals(expected, simple.toString());
    }
}
