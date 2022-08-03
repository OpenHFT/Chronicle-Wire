/*
 * Copyright 2016-2022 chronicle.software
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

package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VanillaMethodReaderHierarchyTest extends WireTestCommon {
    private BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);

    @Test
    public void testInterface() {
        Simple simple = name -> queue.add(name);
        checkWriteRead(simple);
    }

    @Test
    public void testInterfaceDescend() {
        SimpleDescendant simple = name -> queue.add(name);
        checkWriteRead(simple);
    }

    @Test
    public void testDescendantClass() {
        SimpleDescendant simple = new SimpleDescendantClass(queue);
        checkWriteRead(simple);
    }

    @Test
    public void testDescendantAbstractClass() {
        // this was the problem - https://github.com/OpenHFT/Chronicle-Wire/issues/154
        SimpleDescendant simple = new SimpleDescendantClass2(queue);
        checkWriteRead(simple);
    }

    @Test
    public void testDescendantExtendsSameInterface() {
        SimpleDescendant simple = new SimpleDescendantClass3(queue);
        checkWriteRead(simple);
    }

    @Test
    public void testDuckTyping() {
        DuckTyping simple = new DuckTyping(queue);
        checkWriteRead(simple);
    }

    private void checkWriteRead(Simple simple) {
        Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap(32));
        Simple writer = wire.methodWriter(Simple.class);
        MethodReader reader = wire.methodReader(simple);
        final String superMario = "Mario";
        writer.hello(superMario);
        // writer =    "hello: Mario\n...\n"

        assertTrue(reader.readOne());
        assertEquals(1, queue.size());
        assertEquals(superMario, queue.poll());
    }

    interface Simple {
        void hello(String name);
    }

    interface SimpleSameMethod {
        void hello(String name);
    }

    interface SimpleDescendant extends Simple {
    }

    private static class SimpleDescendantClass implements SimpleDescendant {
        private final BlockingQueue<String> queue;
        public SimpleDescendantClass(BlockingQueue<String> queue) {
            this.queue = queue;
        }
        @Override
        public void hello(String name) {
            queue.add(name);
        }
    }

    private static abstract class SimpleAbstractDescendantClass implements SimpleDescendant {
    }

    private static class SimpleDescendantClass2 extends SimpleAbstractDescendantClass {
        private final BlockingQueue<String> queue;
        public SimpleDescendantClass2(BlockingQueue<String> queue) {
            this.queue = queue;
        }
        @Override
        public void hello(String name) {
            queue.add(name);
        }
    }

    private static class SimpleDescendantClass3 extends SimpleDescendantClass2 implements Simple {
        public SimpleDescendantClass3(BlockingQueue<String> queue) {
            super(queue);
        }
    }

    private static class DuckTyping extends SimpleDescendantClass2 implements Simple, SimpleSameMethod {
        public DuckTyping(BlockingQueue<String> queue) {
            super(queue);
        }
    }
}
