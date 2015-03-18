package net.openhft.chronicle.wire;


import net.openhft.chronicle.bytes.Bytes;

import java.util.function.Consumer;

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

    void flip();

    void clear();

    Bytes<?> bytes();

    default boolean readDocument(Consumer<WireIn> metaDataConsumer, Consumer<WireIn> dataConsumer) {
        return Wires.readData(this, metaDataConsumer, dataConsumer);
    }
}
