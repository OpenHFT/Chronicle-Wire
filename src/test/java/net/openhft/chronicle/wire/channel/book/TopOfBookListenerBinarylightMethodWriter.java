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

package net.openhft.chronicle.wire.channel.book;

import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.UpdateInterceptor;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.wire.MarshallableOut;
import net.openhft.chronicle.wire.MethodWriter;
import net.openhft.chronicle.wire.WriteDocumentContext;

import java.util.function.Supplier;

/**
 * Implementation of the {@link net.openhft.chronicle.wire.channel.book.TopOfBookListener}
 * interface which writes method calls as binary messages using Chronicle Wire's binary light
 * wire format.
 * This class also implements {@link MethodWriter} to facilitate the generation and writing
 * of method calls into binary messages.
 */
public final class TopOfBookListenerBinarylightMethodWriter implements net.openhft.chronicle.wire.channel.book.TopOfBookListener, MethodWriter {

    // A closeable resource related to this method writer instance.
    private transient final Closeable closeable;
    // Output sink for marshallable data.
    private transient MarshallableOut out;

    /**
     * Constructor for initializing the method writer with output,
     * closeable resource, and an optional update interceptor.
     *
     * @param out               A supplier providing the output object for marshallable data.
     * @param closeable         A closeable resource associated with this method writer.
     * @param updateInterceptor An interceptor for updates (Not used in this implementation,
     *                          included in case of future use).
     */
    public TopOfBookListenerBinarylightMethodWriter(Supplier<MarshallableOut> out, Closeable closeable, UpdateInterceptor updateInterceptor) {
        // Assign the output source and the closeable resource.
        this.out = out.get();
        this.closeable = closeable;
    }

    /**
     * Set the {@link MarshallableOut} instance which is the destination for the written data.
     *
     * @param out The {@link MarshallableOut} instance.
     */
    @Override
    public void marshallableOut(MarshallableOut out) {
        this.out = out;
    }

    /**
     * Writes the 'topOfBook' method call data to the output as a binary message.
     * Uses a method ID (116) for optimized message identification.
     *
     * @param topOfBook The {@link net.openhft.chronicle.wire.channel.book.TopOfBook}
     *                  instance to be written to the output.
     */
    @MethodId(116)
    public void topOfBook(final net.openhft.chronicle.wire.channel.book.TopOfBook topOfBook) {
        // Get the current output source.
        MarshallableOut out = this.out;
        try (
            // Acquire a context for writing the document to the wire,
            // utilizing Chronicle Wire’s ability to handle the underlying bytes.
            final WriteDocumentContext dc = (WriteDocumentContext) out.acquireWritingDocument(false)
        ) {
            // Write the method name ("topOfBook") and its associated ID (116) to the wire,
            // followed by the marshallable 'topOfBook' data.
            dc.wire()
                    .writeEventId("topOfBook", 116)
                    .marshallable(topOfBook);
        }
    }
}
