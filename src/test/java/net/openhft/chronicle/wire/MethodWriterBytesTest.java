package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

@Ignore("failing test")
public class MethodWriterBytesTest {
    private ArrayBlockingQueue<Bytes> q = new ArrayBlockingQueue(1);

    interface Print {
        void msg(Bytes message);
    }

    @Test
    public void test() throws InterruptedException {
        Wire w = new BinaryWire(Bytes.allocateElasticOnHeap());
        Print printer = w.methodWriter(Print.class);
        printer.msg(Bytes.from("hello"));

        MethodReader reader = w.methodReader((Print) this::println);
        reader.readOne();

        Bytes result = q.poll(10, TimeUnit.SECONDS);
        Assert.assertEquals("hello", result.toString());
    }

    private void println(Bytes bytes) {
        q.add(bytes);
    }

}
