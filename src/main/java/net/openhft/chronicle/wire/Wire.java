package net.openhft.chronicle.wire;

/**
 * The defines the stand interface for writing and reading sequentially to/from a Bytes stream.
 *
 * Created by peter on 12/01/15.
 */
public interface Wire {
    void copyTo(Wire wire);

    WriteValue<Wire> write();

    WriteValue<Wire> writeValue();

    WriteValue<Wire> write(WireKey key);

    WriteValue<Wire> write(CharSequence name, WireKey template);

    ReadValue<Wire> read();

    ReadValue<Wire> read(WireKey key);

    ReadValue<Wire> read(StringBuilder name, WireKey template);

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
}
