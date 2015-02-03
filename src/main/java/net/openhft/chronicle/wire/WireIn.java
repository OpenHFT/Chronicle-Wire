package net.openhft.chronicle.wire;

import net.openhft.lang.io.Bytes;

import java.util.function.Supplier;

/**
 * The defines the stand interface for writing and reading sequentially to/from a Bytes stream. <p> Created by peter on
 * 12/01/15.
 */
public interface WireIn {
    void copyTo(WireOut wire);

    ValueIn read();

    ValueIn read(WireKey key);

    ValueIn read(StringBuilder name, WireKey template);

    boolean hasNextSequenceItem();

    void readSequenceEnd();

    /*
     * read and write comments.
     */
    WireIn readComment(StringBuilder sb);

    boolean hasMapping();

    boolean hasDocument();

    default <T> T readDocument(Supplier<T> reader) {
        return readDocument(reader, null);
    }

    default <T> T readMetaData(Runnable metaDataReader) {
        return readDocument(null, metaDataReader);
    }

    <T> T readDocument(Supplier<T> reader, Runnable metaDataReader);

    void flip();

    void clear();

    Bytes bytes();
}
