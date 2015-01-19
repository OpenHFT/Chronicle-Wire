package net.openhft.chronicle.wire;

import net.openhft.lang.io.Bytes;

/**
 * The defines the stand interface for writing and reading sequentially to/from a Bytes stream.
 *
 * Created by peter on 12/01/15.
 */
public interface Wire {
    void copyTo(Wire wire);

    WriteValue write();

    WriteValue write(WireKey key);

    WriteValue write(CharSequence name, WireKey template);

    WriteValue writeValue();

    ReadValue read();

    ReadValue read(WireKey key);

    ReadValue read(StringBuilder name, WireKey template);

    boolean hasNextSequenceItem();

    void readSequenceEnd();

    /*
     * read and write comments.
     */
    Wire writeComment(CharSequence s);

    Wire readComment(StringBuilder sb);

    boolean hasMapping();

    Wire writeDocumentStart();

    void writeDocumentEnd();

    boolean hasDocument();

    void consumeDocumentEnd();

    void flip();

    void clear();

    Bytes bytes();
}
