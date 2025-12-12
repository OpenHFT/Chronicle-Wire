/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
@Deprecated(/* to be removed in 2027, as it is only used in tests */)
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
