package net.openhft.chronicle.wire;


import net.openhft.chronicle.bytes.Bytes;

import java.util.function.Consumer;

/**
 * The defines the stand interface for writing and reading sequentially to/from a Bytes stream. <p> Created by peter.lawrey on
 * 12/01/15.
 */
public interface WireIn {
    void copyTo(WireOut wire);

    /**
     * Read the field if present, or empty string if not present.
     */
    ValueIn read();

    /**
     * Read the field if present which must match the WireKey.
     */
    ValueIn read(WireKey key);

    /**
     * Read the field if present, or empty string if not present.
     */
    ValueIn read(StringBuilder name);

    /**
     * Read a field, or string which is always written, even for formats which might drop the field.
     */
    default ValueIn readEventName(StringBuilder name) {
        return read(name);
    }

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
