package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TestReadOne {

    static class MyDto extends SelfDescribingMarshallable {
        String data;
    }

    interface MyDtoListener {
        void myDto(MyDto dto);
    }


    static class SnapshotDTO extends SelfDescribingMarshallable {
        String data;
    }

    interface SnapshotListener {
        void snapshot(SnapshotDTO dto);
    }


    @Test
    public void test() throws InterruptedException {

        final Bytes<?> b = Bytes.elasticByteBuffer();
        Wire wire = new TextWire(b) {
            @Override
            public boolean recordHistory() {
                return true;
            }
        };

        MyDtoListener myOut = wire.methodWriterBuilder(MyDtoListener.class).build();
        SnapshotListener snapshotOut = wire.methodWriterBuilder(SnapshotListener.class).build();

        generateHistory(1);
        myOut.myDto(new MyDto());

        generateHistory(2);
        snapshotOut.snapshot(new SnapshotDTO());

        generateHistory(3);
        myOut.myDto(new MyDto());

        generateHistory(4);
        myOut.myDto(new MyDto());

        BlockingQueue<SnapshotDTO> q = new ArrayBlockingQueue<>(128);

        wire.methodReaderBuilder().build(new SnapshotListener() {
            @Override
            public void snapshot(SnapshotDTO d) {
                q.add(d);
            }
        }).readOne();

        Object value = q.poll();

        Assert.assertNotNull(value);
    }

    @NotNull
    private VanillaMessageHistory generateHistory(int value) {
        VanillaMessageHistory messageHistory = (VanillaMessageHistory) MessageHistory.get();
        messageHistory.reset();
        messageHistory.addSource(value, value);
        return messageHistory;
    }

}
