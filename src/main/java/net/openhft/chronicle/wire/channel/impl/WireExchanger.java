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
    static final int USED_MASK = 0x001;
    static final int FREE = 0x000, LOCKED = 0x010, DIRTY = 0x100;
    static final int FREE0 = 0x000, LOCKED0 = 0x010, DIRTY0 = 0x100;
    static final int FREE1 = 0x001, LOCKED1 = 0x011, DIRTY1 = 0x101;
    private static final int INIT_CAPACITY = TCPChronicleChannel.CAPACITY * 2;
    private static final long valueOffset;
    private static final Wire EMPTY_WIRE = WireType.BINARY_LIGHT.apply(Bytes.from(""));

    static {
        try {
            valueOffset = UnsafeMemory.unsafeObjectFieldOffset(
                    WireExchanger.class.getDeclaredField("value"));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private final Wire wire0, wire1;
    private final WEDocumentContext writeContext = new WEDocumentContext();
    private volatile int value;

    public WireExchanger() {
        wire0 = WireType.BINARY_LIGHT.apply(
                Bytes.elasticByteBuffer(INIT_CAPACITY));
        wire0.bytes().singleThreadedCheckDisabled(true);
        wire1 = WireType.BINARY_LIGHT.apply(
                Bytes.elasticByteBuffer(INIT_CAPACITY));
        wire1.bytes().singleThreadedCheckDisabled(true);
    }

    @Override
    protected void performClose() {
        super.performClose();
        wire0.bytes().releaseLast();
        wire1.bytes().releaseLast();
    }

    public Wire acquireProducer() {
        {
            int val = lock();
            int writeTo = val & USED_MASK;
            final Wire wire = wireAt(writeTo);
            if (wire.bytes().readRemaining() <= INIT_CAPACITY / 2) {
                return wire;
            }
            releaseProducer();
        }
        return acquireProducer2();
    }

    @NotNull
    private Wire acquireProducer2() {
        Jvm.pause(1);
        {
            int val2 = lock();
            int writeTo2 = val2 & USED_MASK;
            final Wire wire2 = wireAt(writeTo2);
            final long used = wire2.bytes().readRemaining();
            if (used > INIT_CAPACITY * 3 / 4)
                Jvm.perf().on(getClass(), "Producer buffering " + 100 * used / INIT_CAPACITY + "%");
            return wire2;
        }
    }

    public void releaseProducer() {
        final int val2 = DIRTY | (value & USED_MASK);
        MEMORY.writeOrderedInt(this, valueOffset, val2);
    }

    private Wire wireAt(int writeTo) {
        return writeTo == 0 ? wire0 : wire1;
    }

    public Wire acquireConsumer() {
        if ((value & DIRTY) == 0)
            return EMPTY_WIRE;

        int val = lock();
        int writeTo = val & USED_MASK;
        final int val2 = FREE | writeTo ^ USED_MASK;
        MEMORY.writeOrderedInt(this, valueOffset, val2);

        return wireAt(writeTo);
    }

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

    class WEDocumentContext extends DocumentContextHolder implements WriteDocumentContext {
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

        public void wire(Wire wire) {
            this.wire = wire;
        }
    }
}
