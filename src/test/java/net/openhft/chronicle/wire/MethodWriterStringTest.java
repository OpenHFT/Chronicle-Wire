package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MethodWriterStringTest {
    private ArrayBlockingQueue<String> q = new ArrayBlockingQueue(1);

    interface Print {
        void msg(String message);
    }

    @Test
    public void test() throws InterruptedException {
        Wire w = new BinaryWire(Bytes.allocateElasticOnHeap());
        Print printer = w.methodWriter(Print.class);
        printer.msg("hello");

        MethodReader reader = w.methodReader((Print) this::println);
        reader.readOne();

        String result = q.poll(10, TimeUnit.SECONDS);
        Assert.assertEquals("hello", result);
    }

    private void println(String msg) {
        q.add(msg);
    }

}
