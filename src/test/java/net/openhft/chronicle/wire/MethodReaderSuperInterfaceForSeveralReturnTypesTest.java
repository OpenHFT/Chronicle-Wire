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
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.util.Mocker;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * This class tests the capability of the MethodReader to handle super interfaces with multiple return types.
 * It extends the WireTestCommon which provides utilities for monitoring thread and exception behaviors during tests.
 */
public class MethodReaderSuperInterfaceForSeveralReturnTypesTest extends WireTestCommon {

    /**
     * This test checks the interaction between interfaces with shared super interfaces.
     * It aims to verify that method calls from interfaces `A`, `B`, and `C` (all having a relation to interface `D`)
     * are correctly written to and read from a BinaryWire.
     */
    @Test
    public void test() {
        // Initialization of the wire with padding
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));
        wire.usePadding(true);

        // Create a writer for interface A
        final A writer = wire.methodWriter(A.class);

        // Invoke methods b and c from interface A, followed by the end method from the super interface D
        writer.b().end();
        writer.c().end();

        // StringBuilder to capture intercepted method calls
        StringBuilder sb = new StringBuilder();

        // Create a MethodReader and set up interception of method calls using Mocker
        MethodReader reader = wire.methodReader(Mocker.intercepting(A.class, "*", sb::append));
        assertFalse(reader instanceof VanillaMethodReader);
        assertTrue(reader.readOne());

        // Re-create method reader to nullify previously saved chained call result
        wire.methodReader(Mocker.intercepting(A.class, "*", sb::append));
        assertTrue(reader.readOne());

        // Check if the intercepted method calls were captured correctly
        assertEquals("*b[]*end[]*c[]*end[]", sb.toString());
    }

    /**
     * Interface A defining methods which return instances of interfaces B and C.
     */
    interface A {
        B b();

        C c();
    }

    /**
     * Interface B which extends the super interface D.
     */
    interface B extends D {
    }

    /**
     * Interface C which also extends the super interface D.
     */
    interface C extends D {
    }

    /**
     * Super interface D that declares a single method - end.
     */
    interface D {
        void end();
    }
}
