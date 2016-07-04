package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.IORuntimeException;

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
     * @throws IORuntimeException is the an error occurred while getting the index
     */
    long index() throws IORuntimeException;
}
