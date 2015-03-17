package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.StringBuilderPool;

import java.util.function.Consumer;

import static net.openhft.chronicle.wire.BinaryWire.toIntU30;
import static net.openhft.chronicle.wire.Wire.META_DATA;
import static net.openhft.chronicle.wire.Wire.NOT_READY;

/**
 * Created by peter.lawrey on 16/01/15.
 */
public enum Wires {
    ;
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

    public static void writeData(WireOut wireOut, boolean metaData, Consumer<WireOut> writer) {
        Bytes bytes = wireOut.bytes();
        long position = bytes.position();
        int metaDataBit = metaData ? Wire.META_DATA : 0;
        bytes.writeInt(metaDataBit | Wire.NOT_READY | Wire.UNKNOWN_LENGTH);
        writer.accept(wireOut);
        int length = metaDataBit | toIntU30(bytes.position() - position - 4, "Document length %,d out of 30-bit int range.");
        bytes.writeOrderedInt(position, length);
    }

    public static boolean readData(WireIn wireIn, Consumer<WireIn> metaDataConsumer, Consumer<WireIn> dataConsumer) {
        Bytes bytes = wireIn.bytes();
        boolean read = false;
        while (bytes.remaining() >= 4) {
            long position = bytes.position();
            int length = bytes.readVolatileInt(position);
            if (length == Wire.UNKNOWN_LENGTH || (length & NOT_READY) != 0)
                return read;
            bytes.skip(4);
            int len = length & Wire.LENGTH_MASK;
            if ((length & META_DATA) == 0) {
                if (metaDataConsumer != null) {
                    wireIn.bytes().withLength(len, b -> metaDataConsumer.accept(wireIn));
                    read = true;
                }
            } else {
                wireIn.bytes().withLength(len, b -> dataConsumer.accept(wireIn));
                return true;
            }
        }
        return read;
    }

    public static String fromSizePrefixedBlobs(Bytes bytes) {
        StringBuilder sb = new StringBuilder();
        while (bytes.remaining() >= 4) {
            long length = bytes.readUnsignedInt();
            int len = (int) (length & Wire.LENGTH_MASK);
            String type = (length & META_DATA) != 0 ? "!meta-data!" : "!data!";
            String ready = (length & NOT_READY) != 0 ? " # not ready" : "";
            sb.append("%TAG ").append(type).append(ready).append("\n")
                    .append("---\n");
            for (int i = 0; i < len; i++)
                sb.append((char) bytes.readUnsignedByte());
            if (sb.charAt(sb.length() - 1) != '\n')
                sb.append('\n');
            sb.append("...\n");
        }
        return sb.toString();
    }
}
