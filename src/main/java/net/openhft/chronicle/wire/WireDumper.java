package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;

/**
 * Created by peter on 09/07/16.
 */
public class WireDumper {
    private final WireIn wireIn;
    private final Bytes bytes;
    private long headerNumber = -1;

    private WireDumper(WireIn wireIn, Bytes bytes) {
        if (wireIn == null)
            wireIn = new BinaryWire(bytes);
        this.wireIn = wireIn;
        this.bytes = bytes;
    }

    public static WireDumper of(WireIn wireIn) {
        return new WireDumper(wireIn, wireIn.bytes());
    }

    public static WireDumper of(Bytes bytes) {
        return new WireDumper(new BinaryWire(bytes), bytes);
    }

    public String asString() {
        return asString(bytes.readPosition(), bytes.readRemaining());
    }

    public String asString(long position, long length) {
        StringBuilder sb = new StringBuilder();
        final long limit0 = bytes.readLimit();
        final long position0 = bytes.readPosition();

        try {
            bytes.readPosition(position);
            long limit2 = Math.min(limit0, position + length);
            bytes.readLimit(limit2);
            long missing = position + length - limit2;
            while (bytes.readRemaining() >= 4) {
                if (dumpOne(sb))
                    break;
            }
            if (missing > 0)
                sb.append(" # missing: ").append(missing);
        } catch (Throwable t) {
            sb.append(" ").append(t);
        } finally {
            bytes.readPositionRemaining(position0, limit0 - position0);
        }
        return sb.toString();
    }

    public boolean dumpOne(StringBuilder sb) {
        long start = bytes.readPosition();
        int header = bytes.readInt();
        if (header == 0) {
            sb.append("...\n");
            sb.append("# ").append(bytes.readRemaining()).append(" bytes remaining\n");
            return true;
        }
        if (Wires.isReadyData(header))
            headerNumber++;

        if (start > 0) {
            sb.append("# position: ").append(start).append(", header: ");
            sb.append(headerNumber);
            if (Wires.isNotComplete(header))
                sb.append(" or ").append(headerNumber + 1);
            sb.append("\n");
        }

        int len = Wires.lengthOf(header);
        if (len > bytes.readRemaining()) {
            sb.append("#  has a 4 byte size prefix, ").append(len).append(" > ").append(bytes.readRemaining()).append(" len is ").append(Integer.toString(len));
            return true;
        }
        String type = Wires.isData(header)
                ? Wires.isReady(header) ? "!!data" : "!!not-ready-data!"
                : Wires.isReady(header) ? "!!meta-data" : "!!not-ready-meta-data!";

        byte firstByte = bytes.readByte(bytes.readPosition());
        boolean binary = firstByte < ' ' && firstByte != '\n';

        sb.append("--- ").append(type).append(binary ? " #binary" : "");

        if (len > bytes.readRemaining())
            sb.append(" # len: ").append(len).append(", remaining: ").append(bytes.readRemaining());
        sb.append("\n");
        if (len == 0) {
            sb.append("...\n");
            sb.append("# ").append(bytes.readRemaining()).append(" bytes remaining\n");
            return true;
        }

        Bytes textBytes = bytes;

        if (binary) {
            Bytes bytes2 = Bytes.elasticByteBuffer();
            TextWire textWire = new TextWire(bytes2);
            long readLimit = bytes.readLimit();

            long readPosition = bytes.readPosition();
            try {
                bytes.readLimit(readPosition + len);

                wireIn.copyTo(textWire);
            } catch (Exception e) {
                bytes.readPosition(readPosition);
                throw new IORuntimeException("Unable to parse\n" + bytes.toHexString(Integer.MAX_VALUE), e);
            } finally {
                bytes.readLimit(readLimit);
            }
            textBytes = bytes2;
            len = (int) textBytes.readRemaining();
        }
        try {
            for (int i = 0; i < len; i++) {
                int ch = textBytes.readUnsignedByte();
                sb.append((char) ch);
            }
        } catch (Exception e) {
            sb.append(" ").append(e);
        }
        if (sb.charAt(sb.length() - 1) != '\n')
            sb.append('\n');
        return false;
    }
}
