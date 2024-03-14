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
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.io.SimpleCloseable;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;

import static net.openhft.chronicle.core.UnsafeMemory.MEMORY;

public class WireExchanger extends SimpleCloseable implements MarshallableOut {

    // State constants representing the status of the wires.
    static final int USED_MASK = 0x001;
    static final int FREE = 0x000, LOCKED = 0x010, DIRTY = 0x100;
    static final int FREE0 = 0x000, LOCKED0 = 0x010, DIRTY0 = 0x100;
    static final int FREE1 = 0x001, LOCKED1 = 0x011, DIRTY1 = 0x101;
    private static final int INIT_CAPACITY = TCPChronicleChannel.CAPACITY;
    private static final long valueOffset;

    // Empty wire representation for initialization purposes.
    private static final Wire EMPTY_WIRE = WireType.BINARY_LIGHT.apply(Bytes.from(""));

    static {
        try {
            valueOffset = UnsafeMemory.unsafeObjectFieldOffset(
                    WireExchanger.class.getDeclaredField("value"));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    // The two wires used for data exchange.
    private final Wire wire0, wire1;

    // The document context associated with write operations.
    private final WEDocumentContext writeContext = new WEDocumentContext();
    private int delay = 0;

    // Volatile value used for atomic operations to ensure thread safety.
    private volatile int value;

    /**
     * Initializes the WireExchanger with a default initial capacity.
     */
    public WireExchanger() {
        this(INIT_CAPACITY);
    }

    /**
     * Initializes the WireExchanger with the specified initial capacity.
     *
     * @param initCapacity The initial capacity for the wires.
     */
    @NotNull
    public WireExchanger(int initCapacity) {
        wire0 = WireType.BINARY_LIGHT.apply(
                Bytes.elasticByteBuffer(initCapacity));
        wire0.bytes().singleThreadedCheckDisabled(true);
        wire1 = WireType.BINARY_LIGHT.apply(
                Bytes.elasticByteBuffer(initCapacity));
        wire1.bytes().singleThreadedCheckDisabled(true);
    }

    @Override
    protected void performClose() {
        super.performClose();
        wire0.bytes().releaseLast();
        wire1.bytes().releaseLast();
    }

    /**
     * Acquires a producer wire for data exchange.
     * This method ensures that the wire is not locked or dirty before returning it.
     * If the wire is currently in use, this method will attempt to release the producer and acquire a new one.
     *
     * @return A wire that can be used for producing data.
     */
    public Wire acquireProducer() {
        {
            int val = lock();
            int writeTo = val & USED_MASK;
            final Wire wire = wireAt(writeTo);
            if (wire.bytes().readRemaining() <= INIT_CAPACITY / 2) {
                if (delay > 1)
                    delay--;
                return wire;
            }
            releaseProducer();
        }
        return acquireProducer2();
    }

    /**
     * Acquires a secondary producer wire for data exchange.
     * This method is invoked when the primary wire is found to be in use.
     * The producer will pause momentarily before attempting to access the wire.
     *
     * @return A wire that can be used for producing data.
     */
    @NotNull
    private Wire acquireProducer2() {
        Jvm.pause(delay++);
        {
            int val2 = lock();
            int writeTo2 = val2 & USED_MASK;
            final Wire wire2 = wireAt(writeTo2);
            final long used = wire2.bytes().readRemaining();
            if (used > INIT_CAPACITY * 4L / 5) {
                double ratio = (double) used / INIT_CAPACITY;
                Jvm.perf().on(getClass(), "Producer buffering " + (int) (100 * ratio) + "%");
            }

            return wire2;
        }
    }

    /**
     * Marks the currently locked producer wire as dirty after data has been written into it.
     * This signifies that the wire contains new data that hasn't been read by the consumer yet.
     */
    public void releaseProducer() {
        final int val2 = DIRTY | (value & USED_MASK);
        MEMORY.writeOrderedInt(this, valueOffset, val2);
    }

    /**
     * Returns the wire instance based on the provided index.
     *
     * @param writeTo The index representing which wire to access.
     * @return The wire associated with the provided index.
     */
    private Wire wireAt(int writeTo) {
        return writeTo == 0 ? wire0 : wire1;
    }

    /**
     * Acquires a consumer wire for data reading.
     * If the wire does not have any new data (i.e., it isn't dirty), an empty wire is returned.
     *
     * @return A wire that can be used for consuming data.
     */
    public Wire acquireConsumer() {
        if ((value & DIRTY) == 0)
            return EMPTY_WIRE;

        int val = lock();
        int writeTo = val & USED_MASK;
        final int val2 = FREE | writeTo ^ USED_MASK;
        // assume one consumer
        MEMORY.writeOrderedInt(this, valueOffset, val2);

        return wireAt(writeTo);
    }

    /**
     * Attempts to lock a wire for exclusive access, ensuring thread safety.
     * This method will keep trying to obtain a lock until it succeeds or times out after 10 seconds.
     *
     * @return The value indicating the locked wire's index.
     * @throws IllegalStateException if the locking attempt times out.
     */
    public int lock() throws IllegalStateException {
        long start = System.currentTimeMillis();
        for (; ; Jvm.nanoPause()) {
            int val = value;
            if ((val & LOCKED) != 0) {
                if (System.currentTimeMillis() > start + 10_000)
                    throw new IllegalStateException("timeout");
                continue;
            }
            int writeTo = val & USED_MASK;
            int val2 = LOCKED | writeTo;
            if (MEMORY.compareAndSwapInt(this, valueOffset, val, val2)) {
                return val2;
            }
        }
    }

    /**
     * Releases the currently locked consumer wire.
     * This method is currently a placeholder and may be needed for future implementations.
     */
    public void releaseConsumer() {
        // may be needed in the future
    }

    @Override
    public DocumentContext writingDocument(boolean metaData) {
        final Wire wire = acquireProducer();
        this.writeContext.wire(wire);
        this.writeContext.start(metaData);
        return this.writeContext;
    }

    @Override
    public DocumentContext acquireWritingDocument(boolean metaData) {
        return this.writeContext.documentContext() != null
                && this.writeContext.isOpen()
                && this.writeContext.chainedElement()
                ? this.writeContext
                : this.writingDocument(metaData);
    }

    /**
     * The WEDocumentContext class extends DocumentContextHolder and implements the WriteDocumentContext interface.
     * This class provides a context for writing to a document within the WireExchanger and handles the
     * lifecycle of the document, including its chaining state and closure.
     */
    class WEDocumentContext extends DocumentContextHolder implements WriteDocumentContext {

        // The Wire instance associated with this document context
        private Wire wire;

        @Override
        public void start(boolean metaData) {
            documentContext(wire.writingDocument(metaData));
        }

        @Override
        public boolean chainedElement() {
            return documentContext().chainedElement();
        }

        @Override
        public void chainedElement(boolean chainedElement) {
            documentContext().chainedElement(chainedElement);
        }

        @Override
        public WriteDocumentContext documentContext() {
            return (WriteDocumentContext) super.documentContext();
        }

        @Override
        public void close() {
            final WriteDocumentContext dc = documentContext();
            dc.close();
            if (!dc.isNotComplete()) {
                documentContext(null);
                releaseProducer();
            }
        }

        /**
         * Sets the Wire instance associated with this document context.
         *
         * @param wire The Wire instance to be associated.
         */
        public void wire(Wire wire) {
            this.wire = wire;
        }
    }
}
