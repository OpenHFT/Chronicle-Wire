package net.openhft.chronicle.wire;

import net.openhft.lang.io.Bytes;

/**
 * The defines the stand interface for writing and reading sequentially to/from a Bytes stream.
 * <p/>
 * Created by peter on 12/01/15.
 */
public interface WireOut {
    ValueOut write();

    ValueOut write(WireKey key);

    ValueOut write(CharSequence name, WireKey template);

    ValueOut writeValue();

    /*
     * read and write comments.
     */
    WireOut writeComment(CharSequence s);

    boolean hasMapping();

    WireOut writeDocumentStart();

    void writeDocumentEnd();

    boolean hasDocument();

    void consumeDocumentEnd();

    void flip();

    void clear();

    Bytes bytes();
}
