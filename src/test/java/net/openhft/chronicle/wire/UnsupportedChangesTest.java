package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.onoes.ExceptionKey;
import net.openhft.chronicle.core.onoes.LogLevel;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeFalse;

public class UnsupportedChangesTest extends WireTestCommon {
    @After
    public void reset() {
        Jvm.resetExceptionHandlers();
    }

    @Test
    public void scalarToMarshallable() {
        Map<ExceptionKey, Integer> exceptions = Jvm.recordExceptions(true);

        Nested nested = Marshallable.fromString(Nested.class, "{\n" +
                "inner: 128\n" +
                "}\n");
        assertEquals("!net.openhft.chronicle.wire.UnsupportedChangesTest$Nested {\n" +
                "  inner: !!null \"\"\n" +
                "}\n", nested.toString());
        ExceptionKey ek = new ExceptionKey(LogLevel.WARN,
                WireMarshaller.ObjectFieldAccess.class,
                "Unable to parse field: inner, as a marshallable as it is 128",
                exceptions.keySet().iterator().next().throwable);
        assertEquals(Collections.singletonMap(ek, 1), exceptions);
    }

    @Test
    public void marshallableToScalar() {
        assumeFalse(Jvm.isArm());
        Map<ExceptionKey, Integer> exceptions = Jvm.recordExceptions(true);

        Wrapper wrapper = Marshallable.fromString(Wrapper.class, "{\n" +
                "pnl: { a: 128, b: 1.0 },\n" +
                "second: 123.4," +
                "}\n");
        assertEquals("!net.openhft.chronicle.wire.UnsupportedChangesTest$Wrapper {\n" +
                "  pnl: 0.0,\n" +
                "  second: 123.4\n" +
                "}\n", wrapper.toString());

        assertEquals("{ExceptionKey{level=WARN, clazz=class net.openhft.chronicle.wire.TextWire$TextValueIn, message='Unable to read {a=128, b=1.0} as a double.', throwable=}=1}", exceptions.toString());
    }

    @Test
    public void marshallableToScala2r() {
        Map<ExceptionKey, Integer> exceptions = Jvm.recordExceptions(true);

        IntWrapper wrapper = Marshallable.fromString(IntWrapper.class, "{\n" +
                "pnl: { a: 128, b: 1.0 },\n" +
                "second: 1234," +
                "}\n");
        assertEquals("!net.openhft.chronicle.wire.UnsupportedChangesTest$IntWrapper {\n" +
                "  pnl: 0,\n" +
                "  second: 1234\n" +
                "}\n", wrapper.toString());

        assertEquals("{ExceptionKey{level=WARN, clazz=class net.openhft.chronicle.wire.TextWire$TextValueIn, message='Unable to read {a=128, b=1.0} as a long.', throwable=}=1}", exceptions.toString());
    }

    @Test
    public void marshallableToScalar3() {
        // flag produces a warning.
        BooleanWrapper wrapper = Marshallable.fromString(BooleanWrapper.class, "{\n" +
                "flag: { a: 128, b: 1.0 },\n" +
                "second: 1234," +
                "}\n");
        assertNotNull(wrapper);
    }

    static class Wrapper extends SelfDescribingMarshallable {
        double pnl;
        double second;
    }

    static class IntWrapper extends SelfDescribingMarshallable {
        long pnl;
        long second;
    }

    static class BooleanWrapper extends SelfDescribingMarshallable {
        boolean flag;
        long second;
    }

    static class Nested extends SelfDescribingMarshallable {
        Inner inner;
    }

    static class Inner extends SelfDescribingMarshallable {
        String value;
    }
}
