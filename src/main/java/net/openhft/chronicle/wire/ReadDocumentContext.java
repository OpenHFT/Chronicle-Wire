package net.openhft.chronicle.wire;


public interface ReadDocumentContext extends DocumentContext {
    void start();

    void closeReadLimit(long readLimit);

    void closeReadPosition(long readPosition);
}
