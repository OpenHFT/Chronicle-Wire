/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.Invocation;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.MethodReaderInterceptorReturns;
import net.openhft.chronicle.core.Jvm;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

/** Interface discovery must preserve events when generated dispatch cannot access their types. */
public class MethodReaderInterfaceAccessTest {
    private static final String PROPERTY = "disableCodegenFallback";

    public interface Events {
        void event(int value);
    }

    private interface Helper {
        void helper(int value);
    }

    public static class Receiver implements Events, Helper {
        int eventValue;
        int helperValue;

        public void event(int value) { eventValue = value; }
        public void helper(int value) { helperValue = value; }
    }

    private static class PrivateOuter {
        public interface NestedEvents {
            void helper(int value);
        }
    }

    public static class NestedReceiver implements Events, PrivateOuter.NestedEvents {
        int eventValue;
        int helperValue;

        public void event(int value) { eventValue = value; }
        public void helper(int value) { helperValue = value; }
    }

    public interface InheritedEvents {
        void inherited(int value);
    }

    public interface ChainedEvents {
        void terminal(int value);
    }

    public interface PublicEvents extends InheritedEvents {
        default void doubled(int value) { inherited(2 * value); }
        ChainedEvents chain(int value);
    }

    interface PackageEvents {
        void packageEvent(int value);
    }

    private interface EmptyMarker { }

    public static class PublicReceiver implements PublicEvents, PackageEvents, EmptyMarker {
        final StringBuilder calls = new StringBuilder();
        public void inherited(int value) { calls.append("inherited=").append(value).append(';'); }
        public void packageEvent(int value) { calls.append("package=").append(value).append(';'); }
        public ChainedEvents chain(int value) {
            calls.append("chain=").append(value).append(';');
            return terminal -> calls.append("terminal=").append(terminal).append(';');
        }
    }

    private interface DuplicateEvent {
        void event(int value);
    }

    public static class DuplicateReceiver implements Events, DuplicateEvent {
        int calls;
        public void event(int value) { calls += value; }
    }

    private interface OverloadedHelper {
        void event(String value);
    }

    public static class OverloadedReceiver implements Events, OverloadedHelper {
        public void event(int value) { fail("overloaded event must not be invoked"); }
        public void event(String value) { fail("overloaded event must not be invoked"); }
    }

    public static class Passthrough implements MethodReaderInterceptorReturns {
        public Object intercept(Method method, Object object, Object[] arguments, Invocation invocation) {
            try {
                return invocation.invoke(method, object, arguments);
            } catch (InvocationTargetException e) {
                throw Jvm.rethrow(e);
            }
        }
    }

    @Test
    public void privateHelperPreservesFallbackAndDiagnosesFreshAndCachedRefusal() throws Exception {
        for (String value : new String[]{null, "false", "true", ""})
            run("helper", value);
    }

    @Test
    public void publicMemberOfPrivateOuterIsAlsoInaccessible() throws Exception {
        run("outer", null);
        run("outer", "true");
    }

    @Test
    public void explicitProxyRetainsPublicAndPrivateEventsInStrictMode() throws Exception {
        run("explicit", "true");
    }

    @Test
    public void reflectiveInterceptorPreservesPrivateEventFallback() throws Exception {
        run("helper-interceptor", null);
        run("helper-interceptor", "true");
    }

    @Test
    public void accessibleInheritedDefaultChainedAndDuplicateEventsRemainGenerated() throws Exception {
        run("contracts", null);
        run("contracts", "true");
    }

    @Test
    public void overloadsRemainRejectedInGeneratedAndReflectiveModes() throws Exception {
        run("overloaded", null);
        run("overloaded", "true");
        run("overloaded-proxy", "true");
    }

    public static void main(String[] args) {
        String scenario = args[0];
        if (scenario.equals("explicit") || scenario.equals("overloaded-proxy"))
            System.setProperty("disableReaderProxyCodegen", "true");
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            Wire wire = new BinaryWire(bytes);
            if (scenario.equals("contracts")) {
                publicContracts(wire);
            } else if (scenario.startsWith("overloaded")) {
                IllegalStateException failure = assertThrows(IllegalStateException.class,
                        () -> new VanillaMethodReaderBuilder(wire).build(new OverloadedReceiver()));
                assertTrue(failure.toString(), causeContains(failure, "does not support overloaded methods"));
            } else {
                privateContracts(wire, scenario);
            }
            System.out.println("CHECK_PASSED");
        } finally {
            bytes.releaseLast();
        }
    }

    private static void privateContracts(Wire wire, String scenario) {
        boolean nested = scenario.equals("outer");
        Object receiver = nested ? new NestedReceiver() : new Receiver();
        VanillaMethodReaderBuilder builder = new VanillaMethodReaderBuilder(wire);
        if (scenario.equals("helper-interceptor"))
            builder.methodReaderInterceptorReturns(new Passthrough());
        boolean strict = Jvm.getBoolean(PROPERTY);
        if (strict && !scenario.equals("explicit")) {
            IllegalStateException first = assertThrows(IllegalStateException.class, () -> builder.build(receiver));
            Throwable cause = first.getCause();
            assertTrue(String.valueOf(cause), cause instanceof IllegalStateException);
            assertTrue(cause.getMessage(), cause.getMessage().contains("Cannot generate method-reader dispatch"));
            assertTrue(cause.getMessage(), cause.getMessage().contains("helper"));
            assertTrue(cause.getMessage(), cause.getMessage().contains(nested ? PrivateOuter.class.getName() : Helper.class.getName()));
            assertTrue(cause.getMessage(), cause.getMessage().contains("helper behaviour"));
            IllegalStateException cached = assertThrows(IllegalStateException.class, () -> builder.build(receiver));
            assertTrue(cached.getMessage(), cached.getMessage().contains("previously"));
            assertNull(cached.getCause());
        } else {
            for (int i = 1; i <= 2; i++) {
                try (MethodReader reader = builder.build(receiver)) {
                    assertTrue(reader instanceof VanillaMethodReader);
                    write(wire, "event", i);
                    write(wire, "helper", i + 10);
                    assertTrue(reader.readOne());
                    assertTrue(reader.readOne());
                    assertFalse(reader.readOne());
                    assertEquals(i, nested ? ((NestedReceiver) receiver).eventValue : ((Receiver) receiver).eventValue);
                    assertEquals(i + 10, nested ? ((NestedReceiver) receiver).helperValue : ((Receiver) receiver).helperValue);
                }
            }
            if (!scenario.equals("explicit")) {
                System.setProperty(PROPERTY, "true");
                IllegalStateException cached = assertThrows(IllegalStateException.class, () -> builder.build(receiver));
                assertTrue(cached.getMessage(), cached.getMessage().contains("previously"));
                assertNull(cached.getCause());
            }
        }
        // An inaccessible interface must not prevent unrelated generated readers from working.
        if (!scenario.equals("explicit"))
            publicContracts(wire);
    }

    private static void publicContracts(Wire wire) {
        PublicReceiver receiver = new PublicReceiver();
        Class<?> generated = null;
        for (int i = 0; i < 2; i++) {
            try (MethodReader reader = new VanillaMethodReaderBuilder(wire).build(receiver)) {
                assertTrue(reader instanceof AbstractGeneratedMethodReader);
                if (generated != null)
                    assertSame(generated, reader.getClass());
                generated = reader.getClass();
                write(wire, "inherited", 1);
                write(wire, "doubled", 2);
                write(wire, "packageEvent", 3);
                wire.writeDocument(false, w -> {
                    w.write("chain").int32(4);
                    w.write("terminal").int32(5);
                });
                for (int j = 0; j < 4; j++)
                    assertTrue(reader.readOne());
                assertFalse(reader.readOne());
                assertEquals("inherited=1;inherited=4;package=3;chain=4;terminal=5;", receiver.calls.toString());
                receiver.calls.setLength(0);
            }
        }
        DuplicateReceiver duplicate = new DuplicateReceiver();
        try (MethodReader reader = new VanillaMethodReaderBuilder(wire).build(duplicate)) {
            assertTrue(reader instanceof AbstractGeneratedMethodReader);
            write(wire, "event", 7);
            assertTrue(reader.readOne());
            assertFalse(reader.readOne());
            assertEquals(7, duplicate.calls);
        }
    }

    private static void write(Wire wire, String event, int value) {
        wire.writeDocument(false, w -> w.write(event).int32(value));
    }

    private static boolean causeContains(Throwable failure, String text) {
        for (Throwable t = failure; t != null; t = t.getCause())
            if (String.valueOf(t.getMessage()).contains(text))
                return true;
        return false;
    }

    private static void run(String scenario, String value) throws Exception {
        CodegenFallbackTest.run(scenario, value, true, MethodReaderInterfaceAccessTest.class);
    }
}
