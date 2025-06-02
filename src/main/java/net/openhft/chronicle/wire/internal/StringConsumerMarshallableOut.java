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

package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.*;

import java.util.function.Consumer;

/**
 * An implementation of {@link MarshallableOut} that serializes {@link Marshallable} objects and forwards
 * the serialized string representation to a provided {@link Consumer<String>}.
 * <p>
 * The class encapsulates a {@link Wire} to hold the serialized representation. Upon closing of a document context,
 * the serialized content is converted to a string and passed to the given string consumer.
 */
public class StringConsumerMarshallableOut implements MarshallableOut {

    // Consumer to process the serialized string representation
    private Consumer<String> stringConsumer;

    // Wire object responsible for serialization
    private Wire wire;

    // Document context holder for managing the wire and forwarding the serialized content to the stringConsumer
    private final DocumentContextHolder dcHolder = new DocumentContextHolder() {
        @Override
        public void close() {
            if (chainedElement())
                return;
            super.close();
            if (wire.bytes().isEmpty())
                return;

            stringConsumer.accept(wire.bytes().toString());
            wire.clear();
        }
    };

    /**
     * Constructs a StringConsumerMarshallableOut object with the provided string consumer and wire type.
     *
     * @param stringConsumer The consumer to process serialized string representation.
     * @param wireType       The type of Wire used for serialization.
     */
    public StringConsumerMarshallableOut(Consumer<String> stringConsumer, WireType wireType) {
        this.stringConsumer = stringConsumer;
        this.wire = wireType.apply(Bytes.allocateElasticOnHeap());
    }

    @Override
    public DocumentContext writingDocument(boolean metaData) throws UnrecoverableTimeoutException {
        dcHolder.documentContext(wire.writingDocument(metaData));
        return dcHolder;
    }

    @Override
    public DocumentContext acquireWritingDocument(boolean metaData) throws UnrecoverableTimeoutException {
        dcHolder.documentContext(wire.acquireWritingDocument(metaData));
        return dcHolder;
    }
}
