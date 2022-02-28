/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.core.util.InvocationTargetRuntimeException;
import org.junit.Test;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static net.openhft.chronicle.wire.VanillaMethodReaderBuilder.DISABLE_READER_PROXY_CODEGEN;
import static org.junit.Assert.*;

public class MethodReaderDelegationTest extends WireTestCommon {
    @Test
    public void testUnsuccessfulCallIsDelegatedBinaryWire() {
        final BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());

        doTestUnsuccessfullCallIsDelegated(wire);
    }

    @Test
    public void testUnsuccessfulCallIsDelegatedTextWire() {
        final TextWire wire = new TextWire(Bytes.allocateElasticOnHeap());

        doTestUnsuccessfullCallIsDelegated(wire);
    }

    @Test
    public void testUnsuccessfulCallIsDelegatedYamlWire() {
        final TextWire wire = new TextWire(Bytes.allocateElasticOnHeap());

        doTestUnsuccessfullCallIsDelegated(wire);
    }

    private void doTestUnsuccessfullCallIsDelegated(Wire wire) {
        final MyInterface writer = wire.methodWriter(MyInterface.class);
        writer.myCall();

        try (DocumentContext dc = wire.acquireWritingDocument(false)) {
            Objects.requireNonNull(dc.wire()).writeEventName("myFall").text("");
        }

        writer.myCall();

        AtomicReference<String> delegatedMethodCall = new AtomicReference<>();
        StringBuilder sb = new StringBuilder();

        final MethodReader reader = wire.methodReaderBuilder()
                .defaultParselet((s, in) -> { // Default parselet handling is delegated to Vanilla reader.
                    delegatedMethodCall.set(s.toString());
                    in.skipValue();
                })
                .build(Mocker.intercepting(MyInterface.class, "*", sb::append));

       // System.out.println(WireDumper.of(wire).asString());

        assertTrue(reader.readOne());
        assertNull(delegatedMethodCall.get());

        assertTrue(reader.readOne());
        assertEquals("myFall", delegatedMethodCall.get());

        assertTrue(reader.readOne());

        assertEquals("*myCall[]*myCall[]", sb.toString());
    }

    @Test(expected = InvocationTargetRuntimeException.class)
    public void testUserExceptionsAreNotDelegated() {
        final BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());

        final MyInterface writer = wire.methodWriter(MyInterface.class);

        writer.myCall();

        AtomicInteger exceptionsThrown = new AtomicInteger();

        final MethodReader reader = wire.methodReader((MyInterface) () -> {
            exceptionsThrown.incrementAndGet();

            throw new IllegalStateException("This is an exception by design");
        });

        reader.readOne();
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
        }
        finally {
            System.clearProperty(DISABLE_READER_PROXY_CODEGEN);
        }
    }

    @Test(expected = InvocationTargetRuntimeException.class)
    public void testExceptionThrownFromUserCode() {
        testExceptionThrownFromUserCode(false);
    }

    @Test(expected = InvocationTargetRuntimeException.class)
    public void testExceptionThrownFromUserCodeProxy() {
        testExceptionThrownFromUserCode(true);
    }

    private void testExceptionThrownFromUserCode(boolean proxy) throws InvocationTargetRuntimeException {
        if (proxy)
            System.setProperty(DISABLE_READER_PROXY_CODEGEN, "true");

        try {
            final TextWire wire = new TextWire(Bytes.allocateElasticOnHeap());
            final MyInterface writer = wire.methodWriter(MyInterface.class);
            writer.myCall();

            final MethodReader reader = wire.methodReader((MyInterface) () -> {
                throw new IllegalStateException("This is an exception by design");
            });
            assertEquals(proxy, reader instanceof VanillaMethodReader);

            reader.readOne();
        } finally {
            System.clearProperty(DISABLE_READER_PROXY_CODEGEN);
        }
    }

    @Test(expected = InvocationTargetRuntimeException.class)
    public void testExceptionThrownFromUserCodeLong() {
        testExceptionThrownFromUserCodeLong(false);
    }

    @Test(expected = InvocationTargetRuntimeException.class)
    public void testExceptionThrownFromUserCodeLongProxy() {
        testExceptionThrownFromUserCodeLong(true);
    }

    private void testExceptionThrownFromUserCodeLong(boolean proxy) {
        if (proxy)
            System.setProperty(DISABLE_READER_PROXY_CODEGEN, "true");

        try {
            final TextWire wire = new TextWire(Bytes.allocateElasticOnHeap());
            final MyInterfaceLong writer = wire.methodWriter(MyInterfaceLong.class);
            writer.myCall(1L);

            final MethodReader reader = wire.methodReader((MyInterfaceLong) (l) -> {
                throw new IllegalStateException("This is an exception by design");
            });
            assertEquals(proxy, reader instanceof VanillaMethodReader);

            reader.readOne();
        } finally {
            System.clearProperty(DISABLE_READER_PROXY_CODEGEN);
        }
    }

    interface MyInterface {
        void myCall();
    }

    interface MyInterfaceLong {
        void myCall(long l);
    }
}
