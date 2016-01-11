/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.ReferenceCounted;
import net.openhft.chronicle.core.io.Closeable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by peter.lawrey on 21/09/2015.
 */
public class WiredFile<D extends Marshallable> implements Closeable {
    private static final long TIMEOUT_MS = 10_000; // 10 seconds.

    private final File masterFile;
    private final Function<Bytes, Wire> wireType;
    private final MappedFile mappedFile;
    private final D delegate;
    private final BytesStore headerStore;
    private final long headerLength;
    private final boolean headerCreated;
    private final MappedBytesStoreFactory<WiredMappedBytesStore> mappedBytesStoreFactory;

    public WiredFile(
            @NotNull File masterFile,
            @NotNull Function<Bytes, Wire> wireType,
            @NotNull MappedFile mappedFile,
            @NotNull D delegate,
            @NotNull BytesStore headerStore,
            long headerLength,
            boolean headerCreated,
            @NotNull MappedBytesStoreFactory<WiredMappedBytesStore> mappedBytesStoreFactory) {

        this.masterFile = masterFile;
        this.wireType = wireType;
        this.mappedFile = mappedFile;
        this.delegate = delegate;
        this.headerStore = headerStore;
        this.headerLength = headerLength;
        this.headerCreated = headerCreated;
        this.mappedBytesStoreFactory = mappedBytesStoreFactory;
    }

    public static <D extends Marshallable> WiredFile<D> build(
            String masterFile,
            Function<File, MappedFile> toMappedFile,
            WireType wireType,
            Function<MappedFile, D> delegateSupplier,
            Consumer<WiredFile<D>> consumer) {

        return build(new File(masterFile), toMappedFile, wireType, delegateSupplier, consumer);
    }

    public static <D extends Marshallable> WiredFile<D> build(
            File masterFile,
            Function<File, MappedFile> mappedFileFunction,
            WireType wireType,
            Function<MappedFile, D> delegateSupplier,
            Consumer<WiredFile<D>> installer) {

        File parentFile = masterFile.getParentFile();
        if (parentFile != null) {
            //noinspection ResultOfMethodCallIgnored
            parentFile.mkdirs();
        }

        MappedFile mappedFile = mappedFileFunction.apply(masterFile);
        MappedBytesStoreFactory<WiredMappedBytesStore> mappedBytesStoreFactory = (owner, start, address, capacity, safeCapacity) ->
                new WiredMappedBytesStore(owner, start, address, capacity, safeCapacity, wireType, mappedFile);

        WiredMappedBytesStore header;
        try {
            header = mappedFile.acquireByteStore(0, mappedBytesStoreFactory);
        } catch (IOException e) {
            throw Jvm.rethrow(e);
        }
        assert header != null;
        D delegate;
        long length;
        WiredFile<D> wiredFile;

        //noinspection PointlessBitwiseExpression
        if (header.compareAndSwapInt(0, Wires.NOT_INITIALIZED, Wires.META_DATA | Wires.NOT_READY | Wires.UNKNOWN_LENGTH)) {
            Bytes<?> bytes = header.bytesForWrite().writePosition(4);
            wireType.apply(bytes).getValueOut().typedMarshallable(delegate = delegateSupplier.apply(mappedFile));

            length = bytes.writePosition();

            installer.accept(
                    wiredFile = new WiredFile<>(masterFile, wireType, mappedFile, delegate, header, length, true, mappedBytesStoreFactory)
            );

            header.writeOrderedInt(0L, Wires.META_DATA | Wires.toIntU30(bytes.writePosition() - 4, "Delegate too large"));
        } else {
            long end = System.currentTimeMillis() + TIMEOUT_MS;
            while ((header.readVolatileInt(0) & Wires.NOT_READY) == Wires.NOT_READY) {
                if (System.currentTimeMillis() > end) {
                    throw new IllegalStateException("Timed out waiting for the header record to be ready in " + masterFile);
                }

                Jvm.pause(1);
            }
            Bytes<?> bytes = header.wire.bytes();
            bytes.readPosition(0);
            bytes.writePosition(bytes.capacity());
            int len = Wires.lengthOf(bytes.readVolatileInt());
            bytes.readLimit(length = bytes.readPosition() + len);
            //noinspection unchecked
            delegate = wireType.apply(bytes).getValueIn().typedMarshallable();

            installer.accept(
                    wiredFile = new WiredFile<>(masterFile, wireType, mappedFile, delegate, header, length, false, mappedBytesStoreFactory)
            );
        }

        return wiredFile;
    }

    public File masterFile() {
        return masterFile;
    }

    public MappedFile mappedFile() {
        return mappedFile;
    }

    public D delegate() {
        return delegate;
    }

    public long headerLength() {
        return headerLength;
    }

    public BytesStore headerStore() {
        return this.headerStore;
    }

    public boolean headerCreated() {
        return this.headerCreated;
    }

    public Function<Bytes, Wire> wireSupplier() {
        return this.wireType;
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

        WiredMappedBytesStore(ReferenceCounted owner, long start, long address, long capacity, long safeCapacity, Function<Bytes, Wire> wireType, final MappedFile mappedFile) throws IllegalStateException {
            super(owner, start, address, capacity, safeCapacity, mappedFile);
            this.wire = wireType.apply(this.bytesForWrite());
        }

        public Wire getWire() {
            return wire;
        }
    }
}
