package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
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
        SimpleDescendant simple2 = name -> queue.add(name);
        checkWriteRead(simple2);
    }

    @Test
    public void testDescendantClass() {
        SimpleDescendant simple3 = new SimpleDescendantClass(queue);
        checkWriteRead(simple3);
    }

    @Test
    public void testDescendantAbstractClass() {
        // this was the problem - https://github.com/OpenHFT/Chronicle-Wire/issues/154
        SimpleDescendant simple4 = new SimpleDescendantClass2(queue);
        checkWriteRead(simple4);
    }

    private void checkWriteRead(Simple simple) {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(32));
        Simple writer = wire.methodWriter(Simple.class);
        MethodReader reader = wire.methodReader(simple);
        final String superMario = "Mario";
        writer.hello(superMario);
        assertTrue(reader.readOne());
        assertEquals(1, queue.size());
        assertEquals(superMario, queue.poll());
    }

    interface Simple {
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
}
