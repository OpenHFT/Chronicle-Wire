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
import org.junit.Test;

import static org.junit.Assert.*;

public class MethodReaderSuperInterfaceForSeveralReturnTypesTest extends WireTestCommon {
    @Test
    public void test() {
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));
        wire.usePadding(true);

        final A writer = wire.methodWriter(A.class);

        writer.b().end();
        writer.c().end();

        StringBuilder sb = new StringBuilder();

        MethodReader reader = wire.methodReader(Mocker.intercepting(A.class, "*", sb::append));
        assertFalse(reader instanceof VanillaMethodReader);
        assertTrue(reader.readOne());

        // Re-create method reader to nullify previously saved chained call result
        wire.methodReader(Mocker.intercepting(A.class, "*", sb::append));
        assertTrue(reader.readOne());

        assertEquals("*b[]*end[]*c[]*end[]", sb.toString());
    }

    interface A {
        B b();

        C c();
    }

    interface B extends D {
    }

    interface C extends D {
    }

    interface D {
        void end();
    }
}
