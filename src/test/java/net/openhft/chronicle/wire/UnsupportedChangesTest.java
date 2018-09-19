package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.onoes.ExceptionKey;
import org.junit.After;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/*
 * Created by peter.lawrey@chronicle.software on 31/07/2017
 */
public class UnsupportedChangesTest {
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

        assertEquals("{ExceptionKey{level=WARN, clazz=class net.openhft.chronicle.wire.WireMarshaller$ObjectFieldAccess, message='Unable to parse field: inner, as a marshallable as it is 128', throwable=}=1}", exceptions.toString());
    }

    @Test
    public void marshallableToScalar() {
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
    }

    static class Wrapper extends AbstractMarshallable {
        double pnl;
        double second;
    }

    static class IntWrapper extends AbstractMarshallable {
        long pnl;
        long second;
    }

    static class BooleanWrapper extends AbstractMarshallable {
        boolean flag;
        long second;
    }

    static class Nested extends AbstractMarshallable {
        Inner inner;
    }

    static class Inner extends AbstractMarshallable {
        String value;
    }
}
