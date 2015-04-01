package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static net.openhft.chronicle.wire.BinaryWire.toIntU30;

/**
 * Created by peter.lawrey on 16/01/15.
 */
public enum Wires {
    ;
    public static final int NOT_READY = 1 << 31;
    public static final int META_DATA = 1 << 30;
    public static final int UNKNOWN_LENGTH = 0x0;
    public static final int LENGTH_MASK = -1 >>> 2;

    static final StringBuilderPool SBP = new StringBuilderPool();
    static final StringBuilderPool ASBP = new StringBuilderPool();

    public static StringBuilder acquireStringBuilder() {
        return SBP.acquireStringBuilder();
    }

    public static StringBuilder acquireAnotherStringBuilder(CharSequence cs) {
        StringBuilder sb = ASBP.acquireStringBuilder();
        assert sb != cs;
        return sb;
    }


    private static Consumer<WireIn> emptyMetaDataConsumer = new Consumer<WireIn>() {
        @Override
        public void accept(WireIn wireIn) {
            // skip the meta data
        }
    };

    public static void writeData(WireOut wireOut, boolean metaData, Consumer<WireOut> writer) {
        Bytes bytes = wireOut.bytes();
        long position = bytes.position();
        int metaDataBit = metaData ? META_DATA : 0;
        bytes.writeInt(metaDataBit | NOT_READY | UNKNOWN_LENGTH);
        writer.accept(wireOut);
        int length = metaDataBit | toIntU30(bytes.position() - position - 4, "Document length %,d out of 30-bit int range.");
        bytes.writeOrderedInt(position, length);
    }

    public static boolean readData(@NotNull WireIn wireIn,
                                   @Nullable Consumer<WireIn> metaDataConsumer,
                                   @Nullable Consumer<WireIn> dataConsumer) {

        final Bytes bytes = wireIn.bytes();
        boolean read = false;
        while (bytes.remaining() >= 4) {
            long position = bytes.position();
            int header = bytes.readVolatileInt(position);
            if (!isKnownLength(header))
                return read;
            bytes.skip(4);
            final int len = lengthOf(header);
            if (isData(header)) {
                if (dataConsumer == null) {
                    return false;
                } else {
                    wireIn.bytes().withLength(len, b -> dataConsumer.accept(wireIn));
                    return true;
                }
            } else {

                if (metaDataConsumer == null)
                    // skip the header
                    wireIn.bytes().skip(len);
                else
                    wireIn.bytes().withLength(len, b -> metaDataConsumer.accept(wireIn));

                if (dataConsumer == null)
                    return true;
                read = true;
            }
        }
        return read;
    }

    public static String fromSizePrefixedBlobs(Bytes bytes) {
        long position = bytes.position();
        StringBuilder sb = new StringBuilder();
        while (bytes.remaining() >= 4) {
            long header = bytes.readUnsignedInt();
            int len = lengthOf(header);
            String type = isData(header)
                    ? isReady(header) ? "!!data" : "!!not-ready-data!"
                    : isReady(header) ? "!!meta-data" : "!!not-ready-meta-data!";
            sb.append("--- ").append(type).append("\n");
            for (int i = 0; i < len; i++)
                sb.append((char) bytes.readUnsignedByte());
            if (sb.charAt(sb.length() - 1) != '\n')
                sb.append('\n');
        }
        bytes.position(position);
        return sb.toString();
    }

    public static int lengthOf(long len) {
        return (int) (len & LENGTH_MASK);
    }

    public static boolean isReady(long len) {
        return (len & NOT_READY) == 0;
    }

    public static boolean isData(long len) {
        return (len & META_DATA) == 0;
    }

    public static boolean isKnownLength(long len) {
        return (len & (META_DATA | LENGTH_MASK)) != UNKNOWN_LENGTH;
    }
}
