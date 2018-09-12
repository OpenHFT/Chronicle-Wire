package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.MultiReaderBytesRingBuffer;
import net.openhft.chronicle.bytes.RingBufferReader;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Rob Austin
 */
public interface MultiReaderWireRingBuffer extends MultiReaderBytesRingBuffer {

    default DocumentContext writingDocument() throws UnrecoverableTimeoutException {
        return writingDocument(false);
    }

    DocumentContext writingDocument(boolean metaData) throws UnrecoverableTimeoutException;


    @NotNull
    RingBufferReader createReader();
}
