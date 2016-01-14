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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.Closeable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by peter.lawrey on 21/09/2015.
 */
public class WiredBytes<D extends Marshallable> implements Closeable {
    private static final long TIMEOUT_MS = 10_000; // 10 seconds.

    private final Function<Bytes, Wire> wireType;
    private final MappedBytes mappedBytes;
    private final D delegate;
    private final long headerLength;
    private final boolean headerCreated;

    public WiredBytes(
            @NotNull Function<Bytes, Wire> wireType,
            @NotNull MappedBytes mappedBytes,
            @NotNull D delegate,
            long headerLength,
            boolean headerCreated) {

        this.wireType = wireType;
        this.mappedBytes = mappedBytes;
        this.delegate = delegate;
        this.headerLength = headerLength;
        this.headerCreated = headerCreated;
    }

    public static <D extends Marshallable> WiredBytes<D> build(
            String masterFile,
            Function<File, MappedBytes> toMappedBytes,
            WireType wireType,
            Function<MappedBytes, D> delegateSupplier,
            Consumer<WiredBytes<D>> consumer) {

        return build(new File(masterFile), toMappedBytes, wireType, delegateSupplier, consumer);
    }

    public static <D extends Marshallable> WiredBytes<D> build(
            File masterFile,
            Function<File, MappedBytes> mappedBytesFunction,
            WireType wireType,
            Function<MappedBytes, D> delegateSupplier,
            Consumer<WiredBytes<D>> installer) {

        File parentFile = masterFile.getParentFile();
        if (parentFile != null) {
            //noinspection ResultOfMethodCallIgnored
            parentFile.mkdirs();
        }

        MappedBytes mappedBytes = mappedBytesFunction.apply(masterFile);

        D delegate;
        long length;
        WiredBytes<D> wiredBytes;

        //noinspection PointlessBitwiseExpression
        if (mappedBytes.compareAndSwapInt(0, Wires.NOT_INITIALIZED, Wires.META_DATA | Wires.NOT_READY | Wires.UNKNOWN_LENGTH)) {
            Bytes<?> bytes = mappedBytes.bytesForWrite().writePosition(4);
            wireType.apply(bytes).getValueOut().typedMarshallable(delegate = delegateSupplier.apply(mappedBytes));

            length = bytes.writePosition();

            installer.accept(
                    wiredBytes = new WiredBytes<>(wireType, mappedBytes, delegate, length, true)
            );

            mappedBytes.writeOrderedInt(0L, Wires.META_DATA | Wires.toIntU30(bytes.writePosition() - 4, "Delegate too large"));
        } else {
            long end = System.currentTimeMillis() + TIMEOUT_MS;
            while ((mappedBytes.readVolatileInt(0) & Wires.NOT_READY) == Wires.NOT_READY) {
                if (System.currentTimeMillis() > end) {
                    throw new IllegalStateException("Timed out waiting for the header record to be ready in " + masterFile);
                }

                Jvm.pause(1);
            }


            mappedBytes.readPosition(0);
            mappedBytes.writePosition(mappedBytes.capacity());
            int len = Wires.lengthOf(mappedBytes.readVolatileInt());
            mappedBytes.readLimit(length = mappedBytes.readPosition() + len);
            //noinspection unchecked
            delegate = wireType.apply(mappedBytes).getValueIn().typedMarshallable();

            installer.accept(
                    wiredBytes = new WiredBytes<>(wireType, mappedBytes, delegate, length, false)
            );
        }

        return wiredBytes;
    }

    public D delegate() {
        return delegate;
    }

    public long headerLength() {
        return headerLength;
    }

    public boolean headerCreated() {
        return this.headerCreated;
    }

    public Function<Bytes, Wire> wireSupplier() {
        return this.wireType;
    }

    @Override
    public void close() {
        mappedBytes.close();
    }

    public MappedBytes mappedBytes() {
        return mappedBytes;
    }
}
