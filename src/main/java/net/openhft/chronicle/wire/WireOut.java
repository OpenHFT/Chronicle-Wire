package net.openhft.chronicle.wire;


import net.openhft.chronicle.bytes.Bytes;

/**
 * The defines the stand interface for writing and reading sequentially to/from a Bytes stream.
 * <p>
 * Created by peter.lawrey on 12/01/15.
 */
public interface WireOut {
    ValueOut write();

    ValueOut write(WireKey key);

    /**
     * write a field less value.
     */
    ValueOut writeValue();

    /*
     * read and write comments.
     */
    WireOut writeComment(CharSequence s);

    boolean hasMapping();

    void writeDocument(Runnable writer);

    void writeMetaData(Runnable writer);

    boolean hasDocument();

    Bytes bytes();

    WireOut addPadding(int paddingToAdd);
}
