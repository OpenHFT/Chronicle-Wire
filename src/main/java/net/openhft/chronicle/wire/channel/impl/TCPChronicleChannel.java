/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
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

package net.openhft.chronicle.wire.channel.impl;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.*;
import net.openhft.chronicle.core.util.Mocker;
import net.openhft.chronicle.threads.PauserMode;
import net.openhft.chronicle.wire.*;
import net.openhft.chronicle.wire.channel.*;
import net.openhft.chronicle.wire.converter.NanoTime;

import java.io.IOException;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static net.openhft.chronicle.core.io.Closeable.closeQuietly;
import static net.openhft.chronicle.core.io.ClosedIORuntimeException.newIORuntimeException;

/**
 * This is the TCPChronicleChannel class which provides a channel that communicates
 * over TCP and encapsulates the Chronicle logic for networking, with a focus on
 * initialization, input-output buffer management, and header parsing.
 * The class is designed to work both as an initiator and as an acceptor.
 */
public class TCPChronicleChannel extends AbstractCloseable implements InternalChronicleChannel {

    // Default capacity for the channel buffers
    static final int CAPACITY = Integer.getInteger("tcp.capacity", 2 << 20); // 2 MB
    private static final String HEADER = "header";
    private static final ChannelHeader NO_HEADER = Mocker.ignored(ChannelHeader.class);
    private static final boolean DUMP_YAML = Jvm.getBoolean("dumpYaml");
    private final ReentrantLock lock = new ReentrantLock();
    private final ChronicleChannelCfg<?> channelCfg;
    private final Wire in = createBuffer();
    private final Wire out = createBuffer();
    private final DocumentContextHolder dch = new ConnectionDocumentContextHolder();

    private final Function<ChannelHeader, ChannelHeader> replaceInHeader;
    private final Function<ChannelHeader, ChannelHeader> replaceOutHeader;
    private ChronicleContext chronicleContext;
    private SystemContext systemContext;
    private SocketChannel sc;
    private ChannelHeader headerIn;
    private ChannelHeader headerInToUse;
    private ChannelHeader headerOut;
    private long lastTestMessage;
    private SocketRegistry socketRegistry;
    private boolean privateSocketRegistry;
    private boolean endOfData = false;
    private boolean unsentTestMessage = false;
    private int bufferSize = CAPACITY * 2;
    private Consumer<ChronicleChannel> closeCallback;

    /**
     * Initiator Constructor for TCPChronicleChannel.
     * Initializes a TCPChronicleChannel with given configurations for acting as an initiator.
     *
     * @param channelCfg       Configuration settings for this channel.
     * @param headerOut        Header to be used for outgoing messages.
     * @param socketRegistry   Registry to manage and provide socket channels.
     * @throws InvalidMarshallableException If the given parameters result in invalid marshalling.
     */
    @SuppressWarnings("this-escape")
    public TCPChronicleChannel(ChronicleChannelCfg<?> channelCfg,
                               ChannelHeader headerOut,
                               SocketRegistry socketRegistry) throws InvalidMarshallableException {
        try {
            this.channelCfg = requireNonNull(channelCfg);
            this.headerOut = requireNonNull(headerOut);
            this.socketRegistry = socketRegistry;
            this.replaceInHeader = null;
            this.replaceOutHeader = null;
            this.sc = null;
            assert channelCfg.initiator();
            checkConnected();
        } catch (Throwable t) {
            close();
            throw t;
        }
    }

    /**
     * Acceptor Constructor for TCPChronicleChannel.
     * Initializes a TCPChronicleChannel with given configurations for acting as an acceptor.
     *
     * @param systemContext    Context for the system in which this channel operates.
     * @param channelCfg       Configuration settings for this channel.
     * @param sc               SocketChannel to which this TCPChronicleChannel corresponds.
     * @param replaceInHeader  Function to replace incoming header.
     * @param replaceOutHeader Function to replace outgoing header.
     */
    @SuppressWarnings("this-escape")
    public TCPChronicleChannel(SystemContext systemContext,
                               ChronicleChannelCfg<?> channelCfg,
                               SocketChannel sc,
                               Function<ChannelHeader, ChannelHeader> replaceInHeader,
                               Function<ChannelHeader, ChannelHeader> replaceOutHeader) {
        try {
            this.systemContext = systemContext;
            this.channelCfg = requireNonNull(channelCfg);
            this.sc = requireNonNull(sc);
            this.replaceInHeader = requireNonNull(replaceInHeader);
            this.replaceOutHeader = requireNonNull(replaceOutHeader);

            this.headerOut = null;
            assert !channelCfg.initiator();
        } catch (Throwable t) {
            close();
            throw t;
        }
    }

    /**
     * Validates the header to ensure it adheres to certain conditions.
     * <p>
     * This method performs the following checks:
     * <ul>
     *     <li>The header should be non-negative.</li>
     *     <li>The header value should not indicate oversized data.</li>
     *     <li>The header value should not indicate oversized meta-data.</li>
     * </ul>
     *
     * @param header The header value to be validated.
     * @return True if the header is valid, otherwise exceptions are thrown for invalid conditions.
     * @throws IllegalStateException if the header is not ready or if it's indicating oversized data or meta-data.
     */
    @SuppressWarnings("SameReturnValue")
    static boolean validateHeader(int header) {
        if (header < 0)
            throw new IllegalStateException("Not ready header " + Integer.toUnsignedString(header, 16));
        if (header < 0x4000_0000 && header > 0x20_0000)
            throw new IllegalStateException("Oversized data header " + Integer.toUnsignedString(header, 16));
        if (header > 0x4000_1000)
            throw new IllegalStateException("Oversized meta-data header " + Integer.toUnsignedString(header, 16));
        return true;
    }

    @Override
    public ChronicleChannelCfg<?> channelCfg() {
        return channelCfg;
    }

    /**
     * Initiates the process to flush the data stored in the 'out' buffer.
     */
    void flush() {
        flushOut(out);
    }

    /**
     * Flushes out the data stored in the given wire's buffer.
     * This method writes the data to the associated socket channel until all data is sent.
     *
     * @param out The wire containing the data to be flushed out.
     * @throws IORuntimeException if an error occurs while writing to the socket channel.
     */
    void flushOut(Wire out) {
        @SuppressWarnings("unchecked") final Bytes<ByteBuffer> bytes = (Bytes<ByteBuffer>) out.bytes();
        if (out.bytes().writeRemaining() <= 0)
            return;
        ByteBuffer bb = bytes.underlyingObject();
        assert bb != null;
        ((Buffer)bb).position(Math.toIntExact(bytes.readPosition()));
        bb.limit(Math.toIntExact(bytes.readLimit()));
        while (bb.remaining() > 0) {
            int len;
            try {
                len = sc.write(bb);
            } catch (IOException e) {
                Thread.yield();
                if (isClosing())
                    return;
                throw newIORuntimeException(e);
            }
            if (len < 0)
                throw new ClosedIORuntimeException("Closed");
        }
        out.clear();
    }

    /**
     * Creates a buffer to store data with elastic capacity.
     *
     * @return A new wire instance with the created buffer.
     */
    private Wire createBuffer() {
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer(CAPACITY);
        IOTools.unmonitor(bytes);
        bytes.singleThreadedCheckDisabled(true);
        return WireType.BINARY_LIGHT.apply(bytes);
    }

    @Override
    public DocumentContext readingDocument() throws ClosedIORuntimeException {
        if (unsentTestMessage && out.writingIsComplete())
            testMessage(lastTestMessage);

        final DocumentContext dc = readingDocument0();
//        System.out.println("in - " + Wires.fromSizePrefixedBlobs(dc));
        if (dc.isMetaData()) {
            final Wire wire = dc.wire();
            long pos = wire.bytes().readPosition();
            final String event = wire.readEvent(String.class);
            if ("testMessage".equals(event)) {
                final long testMessage = wire.getValueIn().readLong(NanoTime.INSTANCE);
                unsentTestMessage = testMessage > lastTestMessage;
                lastTestMessage = testMessage;
            }
            wire.bytes().readPosition(pos);
        }

        return dc;
    }

    /**
     * Retrieves a reading document from the wire 'in'.
     * <p>
     * This method checks if the channel is connected and then attempts to retrieve a reading document.
     * If no document is available, the method will perform various checks and modifications on the buffer
     * to ensure efficient reading. It will also handle specific protocol conditions and handle exceptions
     * like detecting an HTTP request or an invalid protocol.
     * </p>
     *
     * @return A document context representing the reading document.
     * @throws ClosedIORuntimeException   if the socket channel is closed while attempting to read.
     * @throws HTTPDetectedException      if an HTTP GET request is detected.
     * @throws InvalidProtocolException   if an invalid protocol signature is detected.
     * @throws IORuntimeException         if any other IO error occurs during the reading process.
     */
    private DocumentContext readingDocument0() {
        checkConnected();
        @SuppressWarnings("unchecked")
        final Bytes<ByteBuffer> bytes = (Bytes<ByteBuffer>) in.bytes();
        if (bytes.readRemaining() == 0)
            bytes.clear();

        // Try to retrieve a reading document from 'in'
        final DocumentContext dc = in.readingDocument();
        if (dc.isPresent())
            return dc;
        // return an isPresent = false on an empty buffer once.
        if (in.bytes().isEmpty() && endOfData) {
            endOfData = false;
            return dc;
        }

        // Compact the bytes if the read position exceeds certain thresholds
        if (bytes.readPosition() * 2 > Math.max(CAPACITY / 2, bytes.readLimit()))
            bytes.compact();

        // Set up the byte buffer for reading from the socket channel
        final ByteBuffer bb = bytes.underlyingObject();
        bb.position(Math.toIntExact(bytes.writePosition()));
        bb.limit(Math.min(bb.capacity(), Math.toIntExact(bytes.writeLimit())));

        // Attempt to read from the socket channel
        int read;
        try {
            read = sc.read(bb);

        } catch (IOException e) {
            close();
            throw newIORuntimeException(e);
        }

        // Handle conditions where the channel is closed
        if (read < 0) {
            close();
            throw new ClosedIORuntimeException("Closed");
        }
        endOfData = true;
        bytes.writeSkip(read);

        // Check the header for specific protocol conditions
        final int header = bytes.readInt(bytes.readPosition());
        if (headerOut == NO_HEADER) {
            // Detect HTTP GET request
            if (header == 0x20544547) {
                throw new HTTPDetectedException("Start of request\n" + bytes);
            }
            // Detect invalid protocol
            if (header >> 16 != 0x4000) {
                throw new InvalidProtocolException("Dump\n" + bytes.toHexString());
            }
        }

        // Validate the header if enough bytes are remaining
        assert bytes.readRemaining() < 4 || validateHeader(header);

        // Dump the content if required
        if (DUMP_YAML)
            System.out.println("in - " + Integer.toUnsignedString(header, 16) + "\n" + Wires.fromSizePrefixedBlobs(in));
        return in.readingDocument();
    }

    /**
     * Ensures that the current socket channel is connected.
     * <p>
     * If the socket channel (sc) is not open, the method attempts to establish a connection based
     * on the host ports provided by the channel configuration (channelCfg). For initiators, it tries to
     * connect to each host-port combination until a successful connection is established or all attempts fail.
     * </p>
     *
     * @throws InvalidMarshallableException if any marshalling error occurs.
     * @throws IllegalStateException        if the current state indicates closure.
     * @throws IllegalArgumentException     if an invalid port is detected.
     * @throws IORuntimeException           if all connection attempts fail or for IO issues.
     */
    synchronized void checkConnected() throws InvalidMarshallableException {
        // Check if socket channel is open and if headerOut is set
        if (sc != null && sc.isOpen()) {
            if (headerOut == null) {
                acceptorRespondToHeader();
            }
            return;
        }
        closeQuietly(sc);

        // Check if in a closing state
        if (isClosing())
            throw new IllegalStateException("Closed");

        final Set<HostPortCfg> hostPorts = channelCfg.hostPorts();

        // Connection initiation logic for initiators
        if (channelCfg.initiator()) {
            boolean success = false;
            Outer:
            for (HostPortCfg hp : hostPorts) {
                // Invalid port check
                if (hp.port() < -1)
                    throw new IllegalArgumentException("Invalid port " + hp.port() + " connecting to " + hp.hostname());

                try {

                    long end = System.nanoTime()
                            + (long) (channelCfg.connectionTimeoutSecs() * 1e9);
                    if (socketRegistry == null) {
                        socketRegistry = new SocketRegistry();
                        privateSocketRegistry = true;
                    }
                    for (int delay = 1; ; delay++) {
                        try {
                            sc = socketRegistry.createSocketChannel(hp.hostname(), hp.port());
                            configureSocket();
                            writeHeader();
                            readHeader();
                            success = true;
                            break Outer;

                        } catch (IOException e) {
                            if (System.nanoTime() > end)
                                throw new IORuntimeException("hostport=" + hp, e);
                            Jvm.pause(delay);
                        }
                    }

                } catch (Exception e) {
                    Jvm.warn().on(getClass(), "failed to connect to host-port=" + hp);
                }

            }
            if (!success)
                throw new IORuntimeException("failed to connect to any of the following " + hostPorts);
        }

        in.clear();
        out.clear();
    }

    /**
     * Configures the current socket channel based on the pauser mode and sets buffer sizes.
     * <p>
     * This method adjusts the blocking mode of the socket channel and also sets the send and receive
     * buffer sizes for the underlying socket. It calculates the total buffer size based on these values.
     * </p>
     *
     * @throws IOException if any IO error occurs during configuration.
     */
    private void configureSocket() throws IOException {
        // Adjust blocking mode based on pauser mode
        if (channelCfg.pauserMode() == PauserMode.busy)
            sc.configureBlocking(false);

        // Adjust socket buffer sizes
        final Socket socket = sc.socket();
        socket.setReceiveBufferSize(CAPACITY);
        socket.setSendBufferSize(CAPACITY);
        bufferSize = socket.getReceiveBufferSize() +
                socket.getSendBufferSize();
    }

    public void closeCallback(Consumer<ChronicleChannel> closeCallback) {
        this.closeCallback = closeCallback;
    }

    @Override
    protected void performClose() {
        try {
            Consumer<ChronicleChannel> c = closeCallback;
            if (c != null)
                c.accept(this);
        } catch (Exception e) {
            Jvm.warn().on(getClass(), e);
        }
        Closeable.closeQuietly(sc);
        if (privateSocketRegistry)
            Closeable.closeQuietly(socketRegistry);
    }

    /**
     * Synchronized method to send an appropriate response header.
     * <p>
     * The method reads an incoming header and decides the appropriate response header to send.
     * If a predefined header exists, it uses that, or else it creates a new response header or
     * a redirection header based on the input.
     * </p>
     *
     * @throws InvalidMarshallableException if any marshalling error occurs during header handling.
     */
    synchronized void acceptorRespondToHeader() throws InvalidMarshallableException {
        headerOut = NO_HEADER;
        readHeader();
        headerInToUse = replaceInHeader.apply(headerIn);
        final ChannelHeader replyHeader = replaceOutHeader.apply(headerInToUse);

        // Decide on the appropriate response header to use
        if (replyHeader == null) {
            if (headerIn instanceof ChannelHandler) // it's a ChannelHeader
                headerOut = ((ChannelHandler) headerIn).responseHeader(chronicleContext);
            else // reject the connection
                headerOut = new RedirectHeader(Collections.emptyList());
        } else { // return the header
            headerOut = replyHeader;
        }

        // Set system context for the response header and write it
        if (systemContext != null)
            headerOut.systemContext(systemContext);
        writeHeader();
    }

    /**
     * Writes the current response header to the wire.
     * <p>
     * This method writes the {@code headerOut} object to the wire within a document context.
     * </p>
     *
     * @throws InvalidMarshallableException if any marshalling error occurs during the write operation.
     */
    private void writeHeader() throws InvalidMarshallableException {
        try (DocumentContext dc = writingDocument(true)) {
            dc.wire().write(HEADER).object(headerOut);
        }
        out.bytes().singleThreadedCheckReset();
    }

    @Override
    public ChannelHeader headerOut() {
        assert headerOut != null;
        return headerOut;
    }

    @Override
    public ChannelHeader headerIn() {
        if (headerIn == null) {
            acceptorRespondToHeader();
        }
        return headerIn;
    }

    @Override
    public ChannelHeader headerInToUse() {
        if (headerInToUse == null) {
            acceptorRespondToHeader();
        }
        return headerInToUse;
    }

    /**
     * Reads the incoming header from the wire.
     * <p>
     * This method continuously reads the wire until a valid header is retrieved or the thread is interrupted.
     * If an unexpected message type is encountered, a warning is issued.
     * </p>
     *
     * @throws InvalidMarshallableException if any marshalling error occurs during the read operation.
     */
    private void readHeader() throws InvalidMarshallableException {
        while (!Thread.currentThread().isInterrupted()) {
            try (DocumentContext dc = readingDocument()) {
                if (!dc.isPresent()) {
                    Thread.yield();
                    continue;
                }
                final String s = dc.wire().readEvent(String.class);
                if (!HEADER.equals(s)) {
                    Jvm.warn().on(getClass(), "Unexpected first message type " + s);
                }
                headerIn = dc.wire().getValueIn().object(ChannelHeader.class);
                break;
            }
        }
        in.bytes().singleThreadedCheckReset();
    }

    @Override
    public DocumentContext writingDocument(boolean metaData) throws UnrecoverableTimeoutException {
        checkConnected();
        lock.lock();

        final DocumentContext dc = out.writingDocument(metaData);
        dch.documentContext(dc);
        return dch;
    }

    /**
     * Getter method for the current connection configuration.
     * <p>
     * Returns the {@code channelCfg} object representing the current connection configuration.
     * </p>
     *
     * @return The {@code ChronicleChannelCfg} object for the current connection.
     */
    public ChronicleChannelCfg<?> connectionCfg() {
        return channelCfg;
    }

    @Override
    public void testMessage(long now) {
        try {
            try (DocumentContext dc = writingDocument(true)) {
                dc.wire().write("testMessage").writeLong(NanoTime.INSTANCE, now);
            }
        } catch (Exception e) {
            if (isClosing()) {
                Jvm.debug().on(getClass(), "Ignoring testMessage exception as it is closing " + e);
                return;
            }
            throw e;
        }
    }

    @Override
    public long lastTestMessage() {
        return lastTestMessage;
    }

    @Override
    public DocumentContext acquireWritingDocument(boolean metaData) throws UnrecoverableTimeoutException {
        checkConnected();
        lock.lock();

        final DocumentContext dc = out.acquireWritingDocument(metaData);
        dch.documentContext(dc);
        return dch;
    }

    @Override
    public boolean supportsEventPoller() {
        return false;
    }

    @Override
    public EventPoller eventPoller() {
        return null;
    }

    @Override
    public ChronicleChannel eventPoller(EventPoller eventPoller) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Wire acquireProducer() {
        lock.lock();
        return out;
    }

    @Override
    public void releaseProducer() {
        flush();
        lock.unlock();
    }

    /**
     * Returns the combined size of the send and receive buffers for the socket connection.
     *
     * @return The combined buffer size in bytes.
     */
    public int bufferSize() {
        return bufferSize;
    }

    @Override
    public boolean recordHistory() {
        if (headerOut instanceof ChannelHandler
                && ((ChannelHandler) headerOut).recordHistory())
            return true;
        return headerInToUse instanceof ChannelHandler
                && ((ChannelHandler) headerInToUse).recordHistory();
    }

    /**
     * Represents a specialized DocumentContextHolder for managing connection-based document context.
     * <p>
     * This holder ensures proper flushing of data upon closure and handles chained elements within a document context.
     * </p>
     */
    private class ConnectionDocumentContextHolder extends DocumentContextHolder implements WriteDocumentContext {
        private boolean chainedElement;

        @Override
        public void close() {
            super.close();
            if (!chainedElement)
                try {
                    flush();
                } catch (ClosedIORuntimeException ignored) {
                    // ignored
                }
            lock.unlock();
        }

        @Override
        public void start(boolean metaData) {

        }

        @Override
        public boolean chainedElement() {
            return chainedElement;
        }

        @Override
        public void chainedElement(boolean chainedElement) {
            this.chainedElement = chainedElement;
            final DocumentContext dc = documentContext();
            if (dc instanceof WriteDocumentContext)
                ((WriteDocumentContext) dc).chainedElement(chainedElement);
        }
    }
}
