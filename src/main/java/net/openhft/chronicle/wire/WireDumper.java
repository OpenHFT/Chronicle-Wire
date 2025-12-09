/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The WireDumper class provides utility methods to obtain a human-readable dump representation of {@link WireIn} content.
 * This class can operate on different WireIn or Bytes inputs and is designed to facilitate debugging and logging.
 */
@SuppressWarnings("rawtypes")
public class WireDumper {

    // The WireIn source whose content is being dumped
    @NotNull
    private final WireIn wireIn;

    // The underlying Bytes instance from {@link #wireIn}
    @NotNull
    private final Bytes<?> bytes;

    // Internal counter for tracking header/document numbers in the dump output
    private long headerNumber = -1;

    /**
     * Private constructor, use the static factory methods such as
     * {@link #of(WireIn)}. If {@code wireIn} is {@code null} the bytes are
     * wrapped in a new {@link BinaryWire}.
     *
     * @param wireIn WireIn instance (nullable)
     * @param bytes  Bytes instance containing the raw data
     */
    private WireDumper(@Nullable WireIn wireIn, @NotNull Bytes<?> bytes) {
        if (wireIn == null)
            wireIn = new BinaryWire(bytes);
        this.wireIn = wireIn;
        this.bytes = bytes;
    }

    /**
     * Creates a {@code WireDumper} for the given {@link WireIn} instance.
     *
     * @param wireIn The WireIn instance to be dumped
     * @return A new WireDumper instance
     */
    @NotNull
    public static WireDumper of(@NotNull WireIn wireIn) {
        return new WireDumper(wireIn, wireIn.bytes());
    }

    /**
     * Creates a {@code WireDumper} for the given {@link Bytes} instance,
     * implicitly treating it as a {@link BinaryWire}. This method uses
     * the default padding alignment.
     *
     * @param bytes The Bytes object containing the raw data to be dumped
     * @return A new WireDumper instance
     */
    @NotNull
    public static WireDumper of(@NotNull Bytes<?> bytes) {
        return of(bytes, AbstractWire.DEFAULT_USE_PADDING);
    }

    /**
     * Creates a {@code WireDumper} for the given {@link Bytes} instance,
     * implicitly treating it as a {@link BinaryWire}. The {@code align}
     * parameter controls whether padding is assumed (see
     * {@link AbstractWire#DEFAULT_USE_PADDING}).
     *
     * @param bytes The Bytes object containing the raw data to be dumped
     * @param align Whether to use padding alignment
     * @return A new WireDumper instance
     */
    @SuppressWarnings("deprecation")
    @NotNull
    public static WireDumper of(@NotNull Bytes<?> bytes, boolean align) {
        final BinaryWire wireIn = new BinaryWire(bytes);
        wireIn.usePadding(align);
        return new WireDumper(wireIn, bytes);
    }

    /**
     * Obtains a string representation of the entire content present in the bytes.
     * This method uses default abbreviated format.
     *
     * @return String representation of the content
     */
    @NotNull
    public String asString() {
        return asString(false);
    }

    /**
     * Obtains a string representation of the content from the current read position.
     * The length of content to be represented is determined by the remaining bytes to be read.
     *
     * @param abbrev Boolean value indicating whether to use abbreviated format
     * @return String representation of the content from the current read position
     */
    @NotNull
    public String asString(boolean abbrev) {
        return asString(bytes.readPosition(), bytes.readRemaining(), abbrev);
    }

    /**
     * Dumps a specific region of the wire content, starting at {@code position}
     * for {@code length} bytes, as a string.
     *
     * @param position The starting byte position in the wire's underlying buffer
     * @param length   The number of bytes to dump from the starting position
     * @return A string representation of the specified wire content region
     */
    @NotNull
    public String asString(long position, long length) {
        return asString(position, length, false);
    }

    /**
     * Dumps a specific region of the wire content, starting at {@code position}
     * for {@code length} bytes, as a string.
     *
     * @param position The starting byte position in the wire's underlying buffer
     * @param length   The number of bytes to dump from the starting position
     * @param abbrev   If true, long content within individual messages might be abbreviated
     * @return A string representation of the specified wire content region
     */
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
            sb.append(' ').append(t);
        } finally {
            bytes.readLimit(limit0);
            bytes.readPosition(position0);
            bytes2.releaseLast();
        }
        return sb.toString();
    }

    /**
     * Reads and dumps a single size-prefixed message from the wire into the
     * supplied {@code sb}. This method uses the default abbreviated format.
     *
     * @param sb     The {@link StringBuilder} to append the dumped message to
     * @param buffer A temporary {@link Bytes} buffer used if the source message is binary
     * @return {@code true} if the end of the wire was reached, {@code false} otherwise
     */
    public boolean dumpOne(@NotNull StringBuilder sb, @Nullable Bytes<?> buffer) {
        return dumpOne(sb, buffer, false);
    }

    /**
     * Reads and dumps a single size-prefixed message from the wire into the
     * supplied {@code sb}.
     *
     * @param sb     The {@link StringBuilder} to append the dumped message to
     * @param buffer A temporary {@link Bytes} buffer used if the source message is binary; may be {@code null}
     * @param abbrev If true, abbreviates content
     * @return {@code true} if the end of the wire was reached, {@code false} otherwise
         */
    public boolean dumpOne(@NotNull StringBuilder sb, @Nullable Bytes<?> buffer, boolean abbrev) {
        // Read position in the byte buffer for extracting the header
        bytes.readPositionForHeader(wireIn.usePadding());
        long start = this.bytes.readPosition();
        int header = this.bytes.readInt();

        // Check for empty header
        if (header == 0) {
            if (!abbrev) {
                sb.append("...\n")
                        .append("# ").append(this.bytes.readRemaining()).append(" bytes remaining\n");
            }
            return true;
        }

        // Update header number if it's ready data
        if (Wires.isReadyData(header))
            headerNumber++;

        // Append position and header information if not abbreviated
        if (start > 0 && !abbrev) {
            sb.append("# position: ").append(start).append(", header: ").append(headerNumber);
            if (Wires.isEndOfFile(header))
                sb.append(" EOF");
            else if (Wires.isNotComplete(header))
                sb.append(" or ").append(headerNumber + 1);
            sb.append('\n');
        }

        // Calculate the length based on the header and check its validity
        int len0 = Wires.lengthOf(header);
        int len = len0;
        if (len > this.bytes.readRemaining()) {
            sb.append("#  has a 4 byte size prefix, ").append(len).append(" > ").append(this.bytes.readRemaining()).append(" len is ").append(len);
            return true;
        }

        // Determine the type of the data
        @NotNull String type = Wires.isData(header)
                ? Wires.isReady(header) ? "!!data" : "!!not-ready-data"
                : Wires.isReady(header) ? "!!meta-data" : "!!not-ready-meta-data";

        boolean binary = false;
        if (Wires.isEndOfFile(header)) {
            binary = true;
        } else {
            // Check for binary data by peeking into the buffer
            for (int i = 0, end = Math.min(32, len0); i < end; i++) {
                byte b = (byte) this.bytes.peekUnsignedByte(this.bytes.readPosition() + i);
                if (b < ' ' && b != '\n') {
                    binary = true;
                    break;
                }
            }
        }

        // Appending type and format if not abbreviated
        if (!abbrev) {
            sb.append("--- ").append(type).append(binary ? " #binary" : "");
            if (len > this.bytes.readRemaining())
                sb.append(" # len: ").append(len).append(", remaining: ").append(this.bytes.readRemaining());
            sb.append('\n');
        }

        // Handling the case where length is zero
        if (len == 0) {
            if (!abbrev) {
                sb.append("...\n")
                        .append("# ").append(this.bytes.readRemaining()).append(" bytes remaining\n");
            }
            return true;
        }

        Bytes<?> textBytes = this.bytes;

        // If data is binary, it's converted to text format
        if (binary) {
            long readPosition = this.bytes.readPosition();
            long readLimit = this.bytes.readLimit();
            int sblen = sb.length();

            try {
                Bytes<?> bytes2 = buffer == null ? Bytes.allocateElasticOnHeap() : buffer.clear();
                @NotNull TextWire textWire = new TextWire(bytes2);

                this.bytes.readLimit(readPosition + len);

                wireIn.copyTo(textWire);

                textBytes = bytes2;
                BytesUtil.combineDoubleNewline(bytes2);
            } catch (Exception e) {
                dumpAsHexadecimal(sb, len, readPosition, sblen);
                return false;

            } finally {
                this.bytes.readLimit(readLimit);
            }

            len = (int) textBytes.readRemaining();
        }
        try {
            // Trim spaces at the end of the textBytes
            for (; len > 0; len--)
                if (textBytes.readUnsignedByte(textBytes.readPosition() + len - 1) != ' ')
                    break;

            // Appending characters to the StringBuilder
            for (int i = 0; i < len; i++) {
                int ch = textBytes.readUnsignedByte();
                sb.append((char) ch);
            }
        } catch (Exception e) {
            sb.append(' ').append(e);
        }

        // Ensure each entry ends with a newline
        if (sb.charAt(sb.length() - 1) != '\n')
            sb.append('\n');

        // If padding is used, adjust the read position accordingly
        if (wireIn.usePadding())
            len0 = (len0 + 3) & ~3;
        this.bytes.readPosition(Math.min(this.bytes.readLimit(), start + 4 + len0));
        return false;
    }

    /**
     * Dumps the content of the byte buffer as a hexadecimal representation into the provided StringBuilder.
     * It positions the bytes at the specified read position, converts the content to hexadecimal, and sets
     * the resulting string to the StringBuilder.
     *
     * @param sb          The {@link StringBuilder} to append the hexadecimal dump to
     * @param len         The number of bytes to read from {@link #bytes} and dump
     * @param readPosition The starting position in {@link #bytes} to read from
     * @param sblen       The length to which {@code sb} should be reset before appending the hex dump
     */
    public void dumpAsHexadecimal(@NotNull StringBuilder sb, int len, long readPosition, int sblen) {
        // Set the read position and remaining length for the byte buffer.
        bytes.readPositionRemaining(readPosition, len);

        // Reset the StringBuilder to the specified length.
        sb.setLength(sblen);

        // Convert the content of the byte buffer to a hexadecimal string and append it to the StringBuilder.
        sb.append(bytes.toHexString(readPosition, Integer.MAX_VALUE));

        // Adjust the read position of the byte buffer after reading.
        bytes.readPosition(readPosition + len);
    }
}
