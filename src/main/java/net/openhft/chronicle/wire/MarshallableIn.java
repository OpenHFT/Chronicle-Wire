package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.ReadBytesMarshallable;
import net.openhft.chronicle.bytes.StopCharTesters;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by peter on 22/04/16.
 * <p>
 * Anything you can read marshallable object from.
 */
public interface MarshallableIn {
    DocumentContext readingDocument();

    /**
     * @param reader user to read the document
     * @return {@code true} if successful
     */
    default boolean readDocument(@NotNull ReadMarshallable reader) {
        try (DocumentContext dc = readingDocument()) {
            if (!dc.isPresent())
                return false;
            reader.readMarshallable(dc.wire());
        }
        return true;
    }

    /**
     * @param reader used to read the document
     * @return {@code true} if successful
     */
    default boolean readBytes(@NotNull ReadBytesMarshallable reader) {
        try (DocumentContext dc = readingDocument()) {
            if (!dc.isPresent())
                return false;
            reader.readMarshallable(dc.wire().bytes());
        }
        return true;
    }

    /**
     * @param using used to read the document
     * @return {@code true} if successful
     */
    default boolean readBytes(@NotNull Bytes using) {
        using.clear();
        try (DocumentContext dc = readingDocument()) {
            if (!dc.isPresent())
                return false;
            using.write(dc.wire().bytes());
        }
        return true;
    }

    /**
     * Read the next message as a String
     *
     * @return the String or null if there is none.
     */
    default String readText() {
        try (DocumentContext dc = readingDocument()) {
            if (!dc.isPresent()) {
                return null;
            }
            StringBuilder sb = Wires.acquireStringBuilder();
            dc.wire().bytes().parse8bit(sb, StopCharTesters.ALL);
            return WireInternal.INTERNER.intern(sb);
        }
    }

    /**
     * Read the next message as  string
     *
     * @param sb to copy the text into
     * @return true if there was a message, or false if not.
     */
    default boolean readText(StringBuilder sb) {
        try (DocumentContext dc = readingDocument()) {
            if (!dc.isPresent()) {
                sb.setLength(0);
                return false;
            }
            dc.wire().getValueIn().text(sb);
        }
        return true;
    }

    /**
     * Read a Map&gt;String, Object&gt; from the content.
     *
     * @return the Map, or null if no message is waiting.
     */
    default Map<String, Object> readMap() {
        try (DocumentContext dc = readingDocument()) {
            if (!dc.isPresent()) {
                return null;
            }
            if (!dc.wire().hasMore())
                return Collections.emptyMap();
            Map<String, Object> ret = new LinkedHashMap<>();
            StringBuilder sb = Wires.acquireStringBuilder();
            while (dc.wire().hasMore()) {
                Object o = dc.wire().readEventName(sb).object();
                ret.put(WireInternal.INTERNER.intern(sb), o);
            }
            return ret;
        }
    }


    /**
     * Reads messages from this tails as methods.  It returns a BooleanSupplier which returns
     *
     * @param objects which implement the methods serialized to the file.
     * @return a reader which will read one Excerpt at a time
     */
    default MethodReader methodReader(Object... objects) {
        return new MethodReader(this, objects);
    }

}
