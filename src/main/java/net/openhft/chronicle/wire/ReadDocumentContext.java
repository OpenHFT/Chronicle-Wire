package net.openhft.chronicle.wire;

/*
 * Created by Peter Lawrey on 17/05/2017.
 */
public interface ReadDocumentContext extends DocumentContext {
    void start();

    void closeReadLimit(long readLimit);

    void closeReadPosition(long readPosition);
}
