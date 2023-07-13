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
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.core.io.*;
import net.openhft.chronicle.threads.PauserMode;
import net.openhft.chronicle.wire.*;
import net.openhft.chronicle.wire.channel.*;
import net.openhft.chronicle.wire.converter.NanoTime;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static net.openhft.chronicle.core.io.Closeable.closeQuietly;
import static net.openhft.chronicle.core.io.ClosedIORuntimeException.newIORuntimeException;

public class TCPChronicleChannel extends AbstractCloseable implements InternalChronicleChannel {
    // tune for message sizes up to this
    static final int CAPACITY = Integer.getInteger("tcp.capacity", 2 << 20); // 2 MB
    private static final String HEADER = "header";
    private static final ChannelHeader NO_HEADER = Mocker.ignored(ChannelHeader.class);
    private static final boolean DUMP_YAML = Jvm.getBoolean("dumpYaml");
    private final ReentrantLock lock = new ReentrantLock();
    private final ChronicleChannelCfg channelCfg;
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

    /**
     * Initiator constructor
     */
    public TCPChronicleChannel(ChronicleChannelCfg channelCfg,
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
     * Acceptor constructor
     */
    public TCPChronicleChannel(SystemContext systemContext,
                               ChronicleChannelCfg channelCfg,
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
    public ChronicleChannelCfg channelCfg() {
        return channelCfg;
    }

    void flush() {
        flushOut(out);
    }

    void flushOut(Wire out) {
        @SuppressWarnings("unchecked") final Bytes<ByteBuffer> bytes = (Bytes) out.bytes();
        if (out.bytes().writeRemaining() <= 0)
            return;
        ByteBuffer bb = bytes.underlyingObject();
        assert bb != null;
        bb.position(Math.toIntExact(bytes.readPosition()));
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

    private DocumentContext readingDocument0() {
        checkConnected();
        @SuppressWarnings("unchecked") final Bytes<ByteBuffer> bytes = (Bytes) in.bytes();
        if (bytes.readRemaining() == 0)
            bytes.clear();
        final DocumentContext dc = in.readingDocument();
        if (dc.isPresent())
            return dc;
        // return an isPresent = false on an empty buffer once.
        if (in.bytes().isEmpty() && endOfData) {
            endOfData = false;
            return dc;
        }
        if (bytes.readPosition() * 2 > Math.max(CAPACITY / 2, bytes.readLimit()))
            bytes.compact();
        final ByteBuffer bb = bytes.underlyingObject();
        bb.position(Math.toIntExact(bytes.writePosition()));
        bb.limit(Math.min(bb.capacity(), Math.toIntExact(bytes.writeLimit())));
        int read;
        try {
            read = sc.read(bb);

        } catch (IOException e) {
            close();
            throw newIORuntimeException(e);
        }
        if (read < 0) {
            close();
            throw new ClosedIORuntimeException("Closed");
        }
        endOfData = true;
        bytes.writeSkip(read);
        final int header = bytes.readInt(bytes.readPosition());
        if (headerOut == NO_HEADER) {
            // HTTP GET
            if (header == 0x20544547) {
                throw new HTTPDetectedException("Start of request\n" + bytes);
            }
            if (header >> 16 != 0x4000) {
                throw new InvalidProtocolException("Dump\n" + bytes.toHexString());
            }
        }
        assert bytes.readRemaining() < 4 || validateHeader(header);
        if (DUMP_YAML)
            System.out.println("in - " + Integer.toUnsignedString(header, 16) + "\n" + Wires.fromSizePrefixedBlobs(in));
        return in.readingDocument();
    }

    synchronized void checkConnected() throws InvalidMarshallableException {
        if (sc != null && sc.isOpen()) {
            if (headerOut == null) {
                acceptorRespondToHeader();
            }
            return;
        }
        closeQuietly(sc);
        if (isClosing())
            throw new IllegalStateException("Closed");

        final Set<HostPortCfg> hostPorts = channelCfg.hostPorts();

        if (channelCfg.initiator()) {
            boolean success = false;
            Outer:
            for (HostPortCfg hp : hostPorts) {

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

    private void configureSocket() throws IOException {
        if (channelCfg.pauserMode() == PauserMode.busy)
            sc.configureBlocking(false);
        final Socket socket = sc.socket();
        socket.setReceiveBufferSize(CAPACITY);
        socket.setSendBufferSize(CAPACITY);
        bufferSize = socket.getReceiveBufferSize() +
                socket.getSendBufferSize();
    }

    @Override
    protected void performClose() {
        Closeable.closeQuietly(sc);
        if (privateSocketRegistry)
            Closeable.closeQuietly(socketRegistry);
    }

    synchronized void acceptorRespondToHeader() throws InvalidMarshallableException {
        headerOut = NO_HEADER;
        readHeader();
        headerInToUse = replaceInHeader.apply(headerIn);
        final ChannelHeader replyHeader = replaceOutHeader.apply(headerInToUse);
        if (replyHeader == null) {
            if (headerIn instanceof ChannelHandler) // it's a ChannelHeader
                headerOut = ((ChannelHandler) headerIn).responseHeader(chronicleContext);
            else // reject the connection
                //noinspection unchecked
                headerOut = new RedirectHeader(Collections.EMPTY_LIST);
        } else { // return the header
            headerOut = replyHeader;
        }
        if (systemContext != null)
            headerOut.systemContext(systemContext);
        writeHeader();
    }

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

    public ChronicleChannelCfg connectionCfg() {
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

    public int bufferSize() {
        return bufferSize;
    }

    private class ConnectionDocumentContextHolder extends DocumentContextHolder implements WriteDocumentContext {
        private boolean chainedElement;

        @Override
        public void close() {
            super.close();
            if (!chainedElement)
                flush();
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
