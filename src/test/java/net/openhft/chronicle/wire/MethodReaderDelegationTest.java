/*
 * Copyright 2016-2020 chronicle.software
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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.core.util.InvocationTargetRuntimeException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.TestCase.assertFalse;
import static net.openhft.chronicle.wire.VanillaMethodReaderBuilder.DISABLE_READER_PROXY_CODEGEN;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class MethodReaderDelegationTest extends WireTestCommon {
    private boolean useMethodId;

    public MethodReaderDelegationTest(boolean useMethodId) {
        this.useMethodId = useMethodId;
    }

    @Parameterized.Parameters(name = "useMethodId={0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[]{false},
                new Object[]{true}
        );
    }

    @Test
    public void testUnsuccessfulCallIsDelegatedBinaryWire() {
        final BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());

        doTestUnsuccessfulCallIsDelegated(wire, false);
    }

    @Test
    public void testUnsuccessfulCallIsDelegatedBinaryWireScanning() {
        final BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());

        doTestUnsuccessfulCallIsDelegated(wire, true);
    }

    @Test
    public void testUnsuccessfulCallIsDelegatedTextWire() {
        final Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        doTestUnsuccessfulCallIsDelegated(wire, false);
    }
    @Test
    public void testUnsuccessfulCallIsDelegatedTextWireScanning() {
        final Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        doTestUnsuccessfulCallIsDelegated(wire, true);
    }

    @Test
    public void testUnsuccessfulCallIsDelegatedYamlWire() {
        final Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        doTestUnsuccessfulCallIsDelegated(wire, false);
    }
    @Test
    public void testUnsuccessfulCallIsDelegatedYamlWireScanning() {
        final Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        doTestUnsuccessfulCallIsDelegated(wire, true);
    }

    private void doTestUnsuccessfulCallIsDelegated(Wire wire, boolean scanning) {
        wire.reset();
        wire.usePadding(true);

        final Class<? extends MyInterface> ifaceClass = useMethodId ? MyInterfaceMethodId.class : MyInterface.class;
        final MyInterface writer = wire.methodWriter(ifaceClass);
        assertFalse(Proxy.isProxyClass(writer.getClass()));
        writer.myCall();

        final int myFallId = 2;
        final String myFall = useMethodId ? Integer.toString(myFallId) : "myFall";

        try (DocumentContext dc = wire.acquireWritingDocument(false)) {
            if (useMethodId) {
                Objects.requireNonNull(dc.wire()).writeEventId(myFallId).text("");
            } else
                Objects.requireNonNull(dc.wire()).writeEventName("myFall").text("");
        }

        writer.myCall();

        AtomicReference<String> delegatedMethodCall = new AtomicReference<>();
        StringBuilder sb = new StringBuilder();

        final MethodReader reader = wire.methodReaderBuilder()
                .scanning(scanning)
                .defaultParselet((s, in) -> {
                    delegatedMethodCall.set(s.toString());
                    in.skipValue();
                })
                .build(Mocker.intercepting(ifaceClass, "*", sb::append));
        assertFalse(Proxy.isProxyClass(reader.getClass()));

        assertTrue(reader.readOne());
        assertNull(delegatedMethodCall.get());

        reader.readOne();
        assertEquals(myFall, delegatedMethodCall.get());
        if (scanning) {
            assertEquals("*myCall[]*myCall[]", sb.toString());
            // unknown methods are skipped
            assertFalse(reader.readOne());
        } else {
            assertTrue(reader.readOne());
            assertEquals("*myCall[]*myCall[]", sb.toString());
        }
    }

    @Test
    public void testUnsuccessfulCallNoDelegate() {
        testUnsuccessfulCallNoDelegate(false, false, false);
    }

    @Test
    public void testUnsuccessfulCallNoDelegateScanning() {
        testUnsuccessfulCallNoDelegate(false, false, true);
    }

    @Test
    public void testUnsuccessfulCallNoDelegateProxy() {
        testUnsuccessfulCallNoDelegate(true, true, false);
    }
    @Test
    public void testUnsuccessfulCallNoDelegateProxyScanning() {
        testUnsuccessfulCallNoDelegate(true, true, true);
    }

    private void testUnsuccessfulCallNoDelegate(boolean proxy, boolean third, boolean scanning) {
        if (proxy)
            System.setProperty(DISABLE_READER_PROXY_CODEGEN, "true");

        try {
            final Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());
            final MyInterface writer = wire.methodWriter(MyInterface.class);
            writer.myCall();

            try (DocumentContext dc = wire.acquireWritingDocument(false)) {
                Objects.requireNonNull(dc.wire()).writeEventName("myFall").text("");
            }

            writer.myCall();

            StringBuilder sb = new StringBuilder();
            final MethodReader reader = wire.methodReaderBuilder()
                    .scanning(scanning)
                    .build(Mocker.intercepting(MyInterface.class, "*", sb::append));

            assertTrue(reader.readOne());
            if (scanning) {
                assertTrue(reader.readOne());
                assertEquals(third, reader.readOne());
                assertEquals("*myCall[]*myCall[]", sb.toString());
                assertFalse(reader.readOne());
            } else {
                reader.readOne();
                assertTrue(reader.readOne());
                assertFalse(reader.readOne());

                assertEquals("*myCall[]*myCall[]", sb.toString());
            }

        } finally {
            System.clearProperty(DISABLE_READER_PROXY_CODEGEN);
        }
    }

    @Test
    public void testUserExceptionsAreNotDelegated() {
        final BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        wire.usePadding(true);

        final Class<? extends MyInterface> ifaceClass = useMethodId ? MyInterfaceMethodId.class : MyInterface.class;
        final MyInterface writer = wire.methodWriter(ifaceClass);

        writer.myCall();

        AtomicInteger exceptionsThrown = new AtomicInteger();

        final MyInterface myInterface = () -> {
            exceptionsThrown.incrementAndGet();

            throw new IllegalStateException("This is an exception by design");
        };
        final MethodReader reader = wire.methodReader(useMethodId ? (MyInterfaceMethodId) () -> myInterface.myCall() : myInterface);

        assertThrows(InvocationTargetRuntimeException.class, () -> reader.readOne());
    }

    // TODO: test below with interceptor

    @Test
    public void testCodeGenerationCanBeDisabled() {
        System.setProperty(DISABLE_READER_PROXY_CODEGEN, "true");

        try {
            final BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());

            final MethodReader reader = wire.methodReader((MyInterface) () -> {
            });

            assertTrue(reader instanceof VanillaMethodReader);
        } finally {
            System.clearProperty(DISABLE_READER_PROXY_CODEGEN);
        }
    }

    @Test
    public void testExceptionThrownFromUserCode() {
        testExceptionThrownFromUserCode(false);
    }

    @Test
    public void testExceptionThrownFromUserCodeProxy() {
        testExceptionThrownFromUserCode(true);
    }

    private void testExceptionThrownFromUserCode(boolean proxy) throws InvocationTargetRuntimeException {
        if (proxy)
            System.setProperty(DISABLE_READER_PROXY_CODEGEN, "true");

        try {
            final Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());
            final Class<? extends MyInterface> ifaceClass = useMethodId ? MyInterfaceMethodId.class : MyInterface.class;
            final MyInterface writer = wire.methodWriter(ifaceClass);
            writer.myCall();

            final MyInterface myInterface = () -> {
                throw new IllegalStateException("This is an exception by design");
            };
            final MethodReader reader = wire.methodReader(useMethodId ? (MyInterfaceMethodId) () -> myInterface.myCall() : myInterface);
            assertEquals(proxy, reader instanceof VanillaMethodReader);

            assertThrows(InvocationTargetRuntimeException.class, () -> reader.readOne());
        } finally {
            System.clearProperty(DISABLE_READER_PROXY_CODEGEN);
        }
    }

    @Test
    public void testExceptionThrownFromUserCodeLong() {
        testExceptionThrownFromUserCodeLong(false);
    }

    @Test
    public void testExceptionThrownFromUserCodeLongProxy() {
        testExceptionThrownFromUserCodeLong(true);
    }

    private void testExceptionThrownFromUserCodeLong(boolean proxy) {
        if (proxy)
            System.setProperty(DISABLE_READER_PROXY_CODEGEN, "true");

        try {
            final Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());
            final Class<? extends MyInterfaceLong> ifaceClass = useMethodId ? MyInterfaceLongMethodId.class : MyInterfaceLong.class;
            final MyInterfaceLong writer = wire.methodWriter(ifaceClass);
            writer.myCall(1L);

            final MyInterfaceLong myInterface = (l) -> {
                throw new IllegalStateException("This is an exception by design");
            };
            final MethodReader reader = wire.methodReader(useMethodId ? (MyInterfaceLongMethodId) (l) -> myInterface.myCall(l) : myInterface);
            assertEquals(proxy, reader instanceof VanillaMethodReader);

            assertThrows(InvocationTargetRuntimeException.class, () -> reader.readOne());
        } finally {
            System.clearProperty(DISABLE_READER_PROXY_CODEGEN);
        }
    }

    interface MyInterface {
        void myCall();
    }

    interface MyInterfaceMethodId extends MyInterface {
        @MethodId(1)
        void myCall();
    }

    interface MyInterfaceLong {
        void myCall(long l);
    }

    interface MyInterfaceLongMethodId extends MyInterfaceLong {
        @MethodId(2)
        void myCall(long l);
    }
}
