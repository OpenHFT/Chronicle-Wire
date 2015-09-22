package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MappedBytesStore;
import net.openhft.chronicle.bytes.MappedBytesStoreFactory;
import net.openhft.chronicle.bytes.MappedFile;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.ReferenceCounted;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.util.ThrowingFunction;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by peter.lawrey on 21/09/2015.
 */
public class WiredFile<D extends Marshallable> implements Closeable {
    private static final long TIMEOUT_MS = 10_000; // 10 seconds.

    private final String masterFile;
    private final Function<Bytes, Wire> wireType;
    private final MappedFile mappedFile;
    private final D delegate;
    private final MappedBytesStore header;
    private final MappedBytesStoreFactory<WiredMappedBytesStore> mappedBytesStoreFactory;

    public WiredFile(String masterFile, Function<Bytes, Wire> wireType, MappedFile mappedFile, D delegate,
                     MappedBytesStore header, MappedBytesStoreFactory<WiredMappedBytesStore> mappedBytesStoreFactory) {
        this.masterFile = masterFile;
        this.wireType = wireType;
        this.mappedFile = mappedFile;
        this.delegate = delegate;
        this.header = header;
        this.mappedBytesStoreFactory = mappedBytesStoreFactory;
    }

    public static <D extends Marshallable> WiredFile<D> build(String masterFile,
                                                              ThrowingFunction<File, IOException, MappedFile> mappedFileFunction,
                                                              Function<Bytes, Wire> wireType,
                                                              Supplier<D> delegateSupplier,
                                                              Consumer<WiredFile<D>> installer) throws IOException {
        File file = new File(masterFile);
        File parentFile = file.getParentFile();
        if (parentFile != null)
            //noinspection ResultOfMethodCallIgnored
            parentFile.mkdirs();

        MappedFile mappedFile = mappedFileFunction.apply(file);
        MappedBytesStoreFactory<WiredMappedBytesStore> mappedBytesStoreFactory = (owner, start, address, capacity, safeCapacity) ->
                new WiredMappedBytesStore(owner, start, address, capacity, safeCapacity, wireType);

        MappedBytesStore header = mappedFile.acquireByteStore(0, mappedBytesStoreFactory);
        assert header != null;
        D delegate;
        //noinspection PointlessBitwiseExpression
        if (header.compareAndSwapInt(0, Wires.NOT_INITIALIZED, Wires.META_DATA | Wires.NOT_READY | Wires.UNKNOWN_LENGTH)) {
            Bytes<?> bytes = header.bytesForWrite().writePosition(4);
            wireType.apply(bytes).getValueOut().typedMarshallable(delegate = delegateSupplier.get());
            header.writeOrderedInt(0L, Wires.META_DATA | Wires.toIntU30(bytes.writePosition() - 4, "Delegate too large"));
        } else {
            long end = System.currentTimeMillis() + TIMEOUT_MS;
            while ((header.readVolatileInt(0) & Wires.NOT_READY) == Wires.NOT_READY) {
                if (System.currentTimeMillis() > end)
                    throw new IllegalStateException("Timed out waiting for the header record to be ready in " + masterFile);
                Jvm.pause(1);
            }
            Bytes<?> bytes = header.bytesForRead().readPosition(0);
            int length = Wires.lengthOf(bytes.readVolatileInt());
            bytes.readLimit(bytes.readPosition() + length);
            //noinspection unchecked
            delegate = (D) wireType.apply(bytes).getValueIn().typedMarshallable();
        }
        WiredFile<D> wiredFile = new WiredFile<>(masterFile, wireType, mappedFile, delegate, header, mappedBytesStoreFactory);
        installer.accept(wiredFile);
        return wiredFile;
    }

    public String masterFile() {
        return masterFile;
    }

    public MappedFile mappedFile() {
        return mappedFile;
    }

    public D delegate() {
        return delegate;
    }

    public Wire acquireWiredChunk(long position) throws IOException {
        WiredMappedBytesStore mappedBytesStore = mappedFile.acquireByteStore(position, mappedBytesStoreFactory);
        return mappedBytesStore.getWire();
    }

    @Override
    public void close() {
        mappedFile.close();
    }

    static class WiredMappedBytesStore extends MappedBytesStore {
        private final Wire wire;

        WiredMappedBytesStore(ReferenceCounted owner, long start, long address, long capacity, long safeCapacity, Function<Bytes, Wire> wireType) throws IllegalStateException {
            super(owner, start, address, capacity, safeCapacity);
            this.wire = wireType.apply(this.bytesForWrite());
        }

        public Wire getWire() {
            return wire;
        }
    }
}
