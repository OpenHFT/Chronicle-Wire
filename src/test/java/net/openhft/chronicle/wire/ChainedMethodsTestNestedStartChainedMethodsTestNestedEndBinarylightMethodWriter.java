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

    // Hold the reference for the Closeable resource
    private transient final Closeable closeable;

    // Supplier for obtaining MarshallableOut instances
    private transient Supplier<MarshallableOut> out;

    // Thread-local container for NestedEnd method writers
    private transient ThreadLocal<ChainedMethodsTest.NestedEnd> methodWriterNestedEndTL;

    // Constructor: Initialize the writer with a MarshallableOut supplier, Closeable resource, and an UpdateInterceptor
    public ChainedMethodsTestNestedStartChainedMethodsTestNestedEndBinarylightMethodWriter(Supplier<MarshallableOut> out, Closeable closeable, UpdateInterceptor updateInterceptor) {
        this.out = out;
        this.closeable = closeable;
        // Instantiate the thread-local with a method writer for the NestedEnd interface
        methodWriterNestedEndTL = ThreadLocal.withInitial(() -> out.get().methodWriter(ChainedMethodsTest.NestedEnd.class));
    }

    // Update the MarshallableOut supplier with a new MarshallableOut instance and remove the existing thread-local value
    @Override
    public void marshallableOut(MarshallableOut out) {
        this.out = () -> out;
        this.methodWriterNestedEndTL.remove();
    }

    // Start method: Acquire writing document and perform the "start" event writing
    public ChainedMethodsTest.NestedEnd start() {
        try (final WriteDocumentContext dc = (WriteDocumentContext) this.out.get().acquireWritingDocument(false)) {
            try {
                // Chain the next element in the document context
                dc.chainedElement(true);

                // Record message history if the MarshallableOut has history recording enabled
                if (out.get().recordHistory()) MessageHistory.writeHistory(dc);

                // Write the "start" event to the wire output
                final ValueOut valueOut = dc.wire().writeEventName("start");
                valueOut.text("");
            } catch (Throwable t) {
                // Rollback changes in case of an error
                dc.rollbackOnClose();
                throw Jvm.rethrow(t);
            }
        }
        // Return the thread-local NestedEnd method writer instance
        return methodWriterNestedEndTL.get();
    }

    // End method: Acquire writing document and perform the "end" event writing
    public void end() {
        try (final WriteDocumentContext dc = (WriteDocumentContext) this.out.get().acquireWritingDocument(false)) {
            try {
                // Chain the next element in the document context
                dc.chainedElement(false);

                // Record message history if the MarshallableOut has history recording enabled
                if (out.get().recordHistory()) MessageHistory.writeHistory(dc);

                // Write the "end" event to the wire output
                final ValueOut valueOut = dc.wire().writeEventName("end");
                valueOut.text("");
            } catch (Throwable t) {
                // Rollback changes in case of an error
                dc.rollbackOnClose();
                throw Jvm.rethrow(t);
            }
        }
    }

}
