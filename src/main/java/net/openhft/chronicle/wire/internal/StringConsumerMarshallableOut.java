package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.*;

import java.util.function.Consumer;

public class StringConsumerMarshallableOut implements MarshallableOut {
    private Consumer<String> stringConsumer;
    private Wire wire;
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
