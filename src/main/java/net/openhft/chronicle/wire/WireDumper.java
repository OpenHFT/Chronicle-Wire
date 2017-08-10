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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

/*
 * Created by Peter Lawrey on 09/07/16.
 */
public class WireDumper {
    @Nullable
    private final WireIn wireIn;
    @NotNull
    private final Bytes bytes;
    private long headerNumber = -1;

    private WireDumper(@Nullable WireIn wireIn, @NotNull Bytes bytes) {
        if (wireIn == null)
            wireIn = new BinaryWire(bytes);
        this.wireIn = wireIn;
        this.bytes = bytes;
    }

    @NotNull
    public static WireDumper of(@NotNull WireIn wireIn) {
        return new WireDumper(wireIn, wireIn.bytes());
    }

    @NotNull
    public static WireDumper of(@NotNull Bytes bytes) {
        return new WireDumper(new BinaryWire(bytes), bytes);
    }

    @NotNull
    public String asString() {
        return asString(bytes.readPosition(), bytes.readRemaining());
    }

    @NotNull
    public String asString(long position, long length) {
        @NotNull StringBuilder sb = new StringBuilder();
        final long limit0 = bytes.readLimit();
        final long position0 = bytes.readPosition();

        Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
        try {
            bytes.readPosition(position);
            long limit2 = Math.min(limit0, position + length);
            bytes.readLimit(limit2);

            long missing = position + length - limit2;
            while (bytes.readRemaining() >= 4) {
                if (dumpOne(sb, bytes2))
                    break;

            }
            if (missing > 0)
                sb.append(" # missing: ").append(missing);
        } catch (Throwable t) {
            sb.append(" ").append(t);
        } finally {
            bytes.readLimit(limit0);
            bytes.readPosition(position0);
            bytes2.release();
        }
        return sb.toString();
    }

    public boolean dumpOne(@NotNull StringBuilder sb) {
        return dumpOne(sb, null);
    }

    public boolean dumpOne(@NotNull StringBuilder sb, @Nullable Bytes<ByteBuffer> buffer) {
        long start = this.bytes.readPosition();
        int header = this.bytes.readInt();
        if (header == 0) {
            sb.append("...\n");
            sb.append("# ").append(this.bytes.readRemaining()).append(" bytes remaining\n");
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
        if (len > this.bytes.readRemaining()) {
            sb.append("#  has a 4 byte size prefix, ").append(len).append(" > ").append(this.bytes.readRemaining()).append(" len is ").append(Integer.toString(len));
            return true;
        }
        @NotNull String type = Wires.isData(header)
                ? Wires.isReady(header) ? "!!data" : "!!not-ready-data!"
                : Wires.isReady(header) ? "!!meta-data" : "!!not-ready-meta-data!";

        boolean binary = false;
        for (int i = 0; i < 4 && i < this.bytes.readRemaining(); i++) {
            byte b = this.bytes.readByte(this.bytes.readPosition() + i);
            if (b < ' ' && b != '\n') {
                binary = true;
                break;
            }
        }

        sb.append("--- ").append(type).append(binary ? " #binary" : "");

        if (len > this.bytes.readRemaining())
            sb.append(" # len: ").append(len).append(", remaining: ").append(this.bytes.readRemaining());
        sb.append("\n");
        if (len == 0) {
            sb.append("...\n");
            sb.append("# ").append(this.bytes.readRemaining()).append(" bytes remaining\n");
            return true;
        }

        Bytes textBytes = this.bytes;

        if (binary) {
            long readPosition = this.bytes.readPosition();
            long readLimit = this.bytes.readLimit();
            int sblen = sb.length();

            try {
                byte firstByte = this.bytes.readByte(this.bytes.readPosition());
                if (firstByte >= 0) {
                    dumpAsHexadecimal(sb, len, readPosition, sblen);
                    return false;
                }

                Bytes bytes2 = buffer == null ? Bytes.elasticByteBuffer() : buffer.clear();
                @NotNull TextWire textWire = new TextWire(bytes2);

                this.bytes.readLimit(readPosition + len);

                wireIn.copyTo(textWire);

                textBytes = bytes2;
            } catch (Exception e) {
                dumpAsHexadecimal(sb, len, readPosition, sblen);
                return false;

            } finally {
                this.bytes.readLimit(readLimit);
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

    public void dumpAsHexadecimal(@NotNull StringBuilder sb, int len, long readPosition, int sblen) {
        bytes.readPositionRemaining(readPosition, len);
        sb.setLength(sblen);
        sb.append(bytes.toHexString(readPosition, Integer.MAX_VALUE));
        bytes.readPosition(readPosition + len);
    }
}
