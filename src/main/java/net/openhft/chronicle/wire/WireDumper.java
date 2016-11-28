/*
 * Copyright 2016 higherfrequencytrading.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;

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
            if (Wires.isEndOfFile(header))
                sb.append(" EOF");
            else if (Wires.isNotComplete(header))
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

        boolean binary = false;
        for (int i = 0; i < 4 && i < bytes.readRemaining(); i++) {
            byte b = bytes.readByte(bytes.readPosition() + i);
            if (b < ' ' && b != '\n') {
                binary = true;
                break;
            }
        }

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
            long readPosition = bytes.readPosition();
            long readLimit = bytes.readLimit();
            int sblen = sb.length();

            try {
                byte firstByte = bytes.readByte(bytes.readPosition());
                if (firstByte >= 0) {
                    dumpAsHexadecimal(sb, len, readPosition, sblen);
                    return false;
                }

                Bytes bytes2 = Bytes.elasticByteBuffer();
                TextWire textWire = new TextWire(bytes2);

                bytes.readLimit(readPosition + len);

                wireIn.copyTo(textWire);

                textBytes = bytes2;
            } catch (Exception e) {
                dumpAsHexadecimal(sb, len, readPosition, sblen);
                return false;

            } finally {
                bytes.readLimit(readLimit);
            }

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

    public void dumpAsHexadecimal(StringBuilder sb, int len, long readPosition, int sblen) {
        bytes.readPositionRemaining(readPosition, len);
        sb.setLength(sblen);
        sb.append(bytes.toHexString(readPosition, Integer.MAX_VALUE));
        bytes.readPosition(readPosition + len);
    }
}
