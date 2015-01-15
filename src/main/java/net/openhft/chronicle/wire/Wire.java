package net.openhft.chronicle.wire;

import java.util.function.Supplier;

/**
 * The defines the stand interface for writing and reading sequentially to/from a Bytes stream.
 *
 * Created by peter on 12/01/15.
 */
public interface Wire {
    WriteValue write();

    WriteValue write(WireKey key);

    WriteValue write(CharSequence name, WireKey template);

    ReadValue read();

    ReadValue read(WireKey key);

    ReadValue read(Supplier<StringBuilder> name, WireKey template);

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

    Wire readDocumentStart();

    void readDocumentEnd();

    void flip();

    void clear();
}
