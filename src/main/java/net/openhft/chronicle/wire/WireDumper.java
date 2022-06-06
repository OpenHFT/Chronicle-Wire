/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("rawtypes")
public class WireDumper {
    @NotNull
    private final WireIn wireIn;
    @NotNull
    private final Bytes<?> bytes;
    private long headerNumber = -1;

    private WireDumper(@Nullable WireIn wireIn, @NotNull Bytes<?> bytes) {
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
    public static WireDumper of(@NotNull Bytes<?> bytes) {
        return of(bytes, AbstractWire.DEFAULT_USE_PADDING);
    }

    @NotNull
    public static WireDumper of(@NotNull Bytes<?> bytes, boolean align) {
        final BinaryWire wireIn = new BinaryWire(bytes);
        wireIn.usePadding(align);
        return new WireDumper(wireIn, bytes);
    }

    @NotNull
    public String asString() {
        return asString(false);
    }

    @NotNull
    public String asString(boolean abbrev) {
        return asString(bytes.readPosition(), bytes.readRemaining(), abbrev);
    }

    @NotNull
    public String asString(long position, long length) {
        return asString(position, length, false);
    }

    @NotNull
    public String asString(long position, long length, boolean abbrev) {
        @NotNull StringBuilder sb = new StringBuilder();
        final long limit0 = bytes.readLimit();
        final long position0 = bytes.readPosition();

        Bytes<?> bytes2 = Bytes.allocateElasticOnHeap();
        try {
            bytes.readPosition(position);
            long limit2 = Math.min(limit0, position + length);
            bytes.readLimit(limit2);

            long missing = position + length - limit2;
            while (bytes.readRemaining() >= 4) {
                if (dumpOne(sb, bytes2, abbrev))
                    break;

            }
            if (missing > 0 && !abbrev)
                sb.append(" # missing: ").append(missing);
        } catch (Throwable t) {
            sb.append(" ").append(t);
        } finally {
            bytes.readLimit(limit0);
            bytes.readPosition(position0);
            bytes2.releaseLast();
        }
        return sb.toString();
    }

    public boolean dumpOne(@NotNull StringBuilder sb, @Nullable Bytes<?> buffer) {
        return dumpOne(sb, buffer, false);
    }

    public boolean dumpOne(@NotNull StringBuilder sb, @Nullable Bytes<?> buffer, boolean abbrev) {
        bytes.readPositionForHeader(wireIn.usePadding());
        long start = this.bytes.readPosition();
        if (start == 2988)
            Thread.yield();
        int header = this.bytes.readInt();
        if (header == 0) {
            if (!abbrev) {
                sb.append("...\n");
                sb.append("# ").append(this.bytes.readRemaining()).append(" bytes remaining\n");
            }
            return true;
        }
        if (Wires.isReadyData(header))
            headerNumber++;

        if (start > 0 && !abbrev) {
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
            sb.append("#  has a 4 byte size prefix, ").append(len).append(" > ").append(this.bytes.readRemaining()).append(" len is ").append(len);
            return true;
        }
        @NotNull String type = Wires.isData(header)
                ? Wires.isReady(header) ? "!!data" : "!!not-ready-data"
                : Wires.isReady(header) ? "!!meta-data" : "!!not-ready-meta-data";

        boolean binary = false;
        if (Wires.isEndOfFile(header)) {
            binary = true;
        } else {
            for (int i = 0; i < 4 && i < this.bytes.readRemaining(); i++) {
                byte b = (byte) this.bytes.peekUnsignedByte(this.bytes.readPosition() + i);
                if (b < ' ' && b != '\n') {
                    binary = true;
                    break;
                }
            }
        }

        if (!abbrev) {
            sb.append("--- ").append(type).append(binary ? " #binary" : "");
            if (len > this.bytes.readRemaining())
                sb.append(" # len: ").append(len).append(", remaining: ").append(this.bytes.readRemaining());
            sb.append("\n");
        }

        if (len == 0) {
            if (!abbrev) {
                sb.append("...\n");
                sb.append("# ").append(this.bytes.readRemaining()).append(" bytes remaining\n");
            }
            return true;
        }

        Bytes<?> textBytes = this.bytes;

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

                Bytes<?> bytes2 = buffer == null ? Bytes.elasticByteBuffer() : buffer.clear();
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
            // trim spaces
            for (; len > 0; len--)
                if (textBytes.readUnsignedByte(textBytes.readPosition() + len - 1) != ' ')
                    break;
            for (int i = 0; i < len; i++) {
                int ch = textBytes.readUnsignedByte();
                sb.append((char) ch);
            }
        } catch (Exception e) {
            sb.append(" ").append(e);
        }
        if (sb.charAt(sb.length() - 1) != '\n')
            sb.append('\n');
        this.bytes.readPosition(start + 4 + Wires.lengthOf(header));
        return false;
    }

    public void dumpAsHexadecimal(@NotNull StringBuilder sb, int len, long readPosition, int sblen) {
        bytes.readPositionRemaining(readPosition, len);
        sb.setLength(sblen);
        sb.append(bytes.toHexString(readPosition, Integer.MAX_VALUE));
        bytes.readPosition(readPosition + len);
    }
}
