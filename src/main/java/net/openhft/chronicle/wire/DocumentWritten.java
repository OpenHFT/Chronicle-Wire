package net.openhft.chronicle.wire;

public interface DocumentWritten {
    /**
     * @return a context to use in a try-with-resource block
     */
    DocumentContext writingDocument();

    /**
     * Start a new DocumentContext, must always call close() when done.
     */
    DocumentContext writingDocument(boolean metaData) throws UnrecoverableTimeoutException;

    /**
     * Start or reuse an existing a DocumentContext, optionally call close() when done.
     */
    DocumentContext acquireWritingDocument(boolean metaData) throws UnrecoverableTimeoutException;
}
