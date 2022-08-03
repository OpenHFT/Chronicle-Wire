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

package net.openhft.chronicle.wire;

// NOTE: Added to check backward compatibility

import net.openhft.chronicle.bytes.UpdateInterceptor;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.Closeable;

import java.util.function.Supplier;

public final class ChainedMethodsTestNestedStartChainedMethodsTestNestedEndBinarylightMethodWriter implements ChainedMethodsTest.NestedStart, ChainedMethodsTest.NestedEnd, MethodWriter {

    // result
    private transient final Closeable closeable;
    private transient Supplier<MarshallableOut> out;
    private transient ThreadLocal<ChainedMethodsTest.NestedEnd> methodWriterNestedEndTL;

    // constructor
    public ChainedMethodsTestNestedStartChainedMethodsTestNestedEndBinarylightMethodWriter(Supplier<MarshallableOut> out, Closeable closeable, UpdateInterceptor updateInterceptor) {
        this.out = out;
        this.closeable = closeable;
        methodWriterNestedEndTL = ThreadLocal.withInitial(() -> out.get().methodWriter(ChainedMethodsTest.NestedEnd.class));
    }

    @Override
    public void marshallableOut(MarshallableOut out) {
        this.out = () -> out;
        this.methodWriterNestedEndTL.remove();
    }

    public ChainedMethodsTest.NestedEnd start() {
        try (final WriteDocumentContext dc = (WriteDocumentContext) this.out.get().acquireWritingDocument(false)) {
            try {
                dc.chainedElement(true);
                if (out.get().recordHistory()) MessageHistory.writeHistory(dc);
                final ValueOut valueOut = dc.wire().writeEventName("start");
                valueOut.text("");
            } catch (Throwable t) {
                dc.rollbackOnClose();
                throw Jvm.rethrow(t);
            }
        }
        // method return
        return methodWriterNestedEndTL.get();
    }

    public void end() {
        try (final WriteDocumentContext dc = (WriteDocumentContext) this.out.get().acquireWritingDocument(false)) {
            try {
                dc.chainedElement(false);
                if (out.get().recordHistory()) MessageHistory.writeHistory(dc);
                final ValueOut valueOut = dc.wire().writeEventName("end");
                valueOut.text("");
            } catch (Throwable t) {
                dc.rollbackOnClose();
                throw Jvm.rethrow(t);
            }
        }
    }

}
