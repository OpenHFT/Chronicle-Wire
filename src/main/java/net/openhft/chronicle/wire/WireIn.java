package net.openhft.chronicle.wire;

import net.openhft.lang.io.Bytes;

/**
 * The defines the stand interface for writing and reading sequentially to/from a Bytes stream.
 * <p/>
 * Created by peter on 12/01/15.
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

    void consumeDocumentEnd();

    void flip();

    void clear();

    Bytes bytes();
}
