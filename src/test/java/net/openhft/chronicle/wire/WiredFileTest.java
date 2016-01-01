package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MappedFile;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.values.IntValue;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

/**
 * Created by peter on 22/09/15.
 */
public class WiredFileTest {

    final Function<File, MappedFile> toMappedFile = file -> {
        try {
            return MappedFile.mappedFile(file, 64 << 10, 0);
        } catch (FileNotFoundException e) {
            throw Jvm.rethrow(e);
        }
    };

    final Consumer<WiredFile<MyHeader_1_0>> consumer = wiredFile -> wiredFile.delegate().install(wiredFile);

    @Test
    public void testBuildText() throws IOException {
        // use a class alias for MyHeader_1_0
        ClassAliasPool.CLASS_ALIASES.addAlias(MyHeader_1_0.class, "MyHeader-1.0");

        String masterFile = OS.TARGET + "/wired-file-" + System.nanoTime();
        for (int i = 1; i <= 5; i++) {

            WiredFile<MyHeader_1_0> wf = WiredFile.build(masterFile,
                    toMappedFile,
                    WireType.TEXT,
                    x -> new MyHeader_1_0(),
                    consumer
            );

            MyHeader_1_0 header = wf.delegate();
            assertEquals(i, header.installCount.getValue());
            Bytes<?> bytes = wf.acquireWiredChunk(0).bytes();
            bytes.readPosition(0);
            bytes.readLimit(wf.headerLength());
            System.out.println(Wires.fromSizePrefixedBlobs(bytes));
            wf.close();
        }
    }

    @Test
    public void testBuild() throws IOException {
        // use a class alias for MyHeader_1_0
        ClassAliasPool.CLASS_ALIASES.addAlias(MyHeader_1_0.class, "MyHeader-1.0");

        String masterFile = OS.TARGET + "/wired-file-" + System.nanoTime();
        for (int i = 1; i <= 5; i++) {
            WiredFile<MyHeader_1_0> wf = WiredFile.build(masterFile,
                    toMappedFile,
                    WireType.BINARY,
                    x -> new MyHeader_1_0(),
                    consumer
            );
            MyHeader_1_0 header = wf.delegate();
            assertEquals(i, header.installCount.getValue());
            Bytes<?> bytes = wf.acquireWiredChunk(0).bytes();
            bytes.readPosition(0);
            bytes.readLimit(wf.headerLength());
            System.out.println(Wires.fromSizePrefixedBlobs(bytes));
            wf.close();
        }
    }

    static class MyHeader_1_0 implements Marshallable {
        IntValue installCount;

        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            wire.read(() -> "install-count").int32(null, this, (t, i) -> t.installCount = i);
        }

        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            wire.write(() -> "install-count").int32forBinding(0, installCount = wire.newIntReference());

        }

        public void install(WiredFile<MyHeader_1_0> wiredFile) {
            installCount.addAtomicValue(1);
        }
    }
}