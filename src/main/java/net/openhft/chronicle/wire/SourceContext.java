package net.openhft.chronicle.wire;

/**
 * Created by peter on 12/05/16.
 */
public interface SourceContext {
    /**
     * @return the current source id or -1
     */
    int sourceId();

    /**
     * Index last read, only available for read contexts.
     *
     * @return the current Excerpt's index
     */
    long index();
}
