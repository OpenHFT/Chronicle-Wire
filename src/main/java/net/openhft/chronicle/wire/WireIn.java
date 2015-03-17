package net.openhft.chronicle.wire;


import net.openhft.chronicle.bytes.Bytes;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The defines the stand interface for writing and reading sequentially to/from a Bytes stream. <p> Created by peter.lawrey on
 * 12/01/15.
 */
public interface WireIn {
    void copyTo(WireOut wire);

    ValueIn read();

    ValueIn read(WireKey key);

    ValueIn read(StringBuilder name);

    boolean hasNextSequenceItem();

    /*
     * read and write comments.
     */
    WireIn readComment(StringBuilder sb);

    boolean hasMapping();

    boolean hasDocument();

    <T> T readDocument(Function<WireIn, T> reader, Consumer<WireIn> metaDataReader);

    void flip();

    void clear();

    Bytes<?> bytes();
}
