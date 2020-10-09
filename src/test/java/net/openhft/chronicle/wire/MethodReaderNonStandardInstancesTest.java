/*
 * Copyright 2016-2020 Chronicle Software
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
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MethodReaderNonStandardInstancesTest {
    @Test
    public void testAnonymousClassCanBePassedToMethodReader() {
        BinaryWire binaryWire = new BinaryWire(Bytes.allocateElasticOnHeap(128));

        MyInterface writer = binaryWire.methodWriter(MyInterface.class);

        writer.call();

        AtomicBoolean b = new AtomicBoolean();

        MethodReader reader = binaryWire.methodReader(new MyInterface() {
            @Override
            public void call() {
                b.set(true);
            }
        });

        assertFalse(reader instanceof VanillaMethodReader);

        assertTrue(reader.readOne());
        assertTrue(b.get());
    }

    @Test
    public void testLambdaCanBePassedToMethodReader() {
        BinaryWire binaryWire = new BinaryWire(Bytes.allocateElasticOnHeap(128));

        MyInterface writer = binaryWire.methodWriter(MyInterface.class);

        writer.call();

        AtomicBoolean b = new AtomicBoolean();

        MethodReader reader = binaryWire.methodReader((MyInterface) () -> b.set(true));

        assertFalse(reader instanceof VanillaMethodReader);

        assertTrue(reader.readOne());
        assertTrue(b.get());
    }

    interface MyInterface {
        void call();
    }
}
