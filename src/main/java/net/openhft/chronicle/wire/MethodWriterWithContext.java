/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire;

public interface MethodWriterWithContext {
    /**
     * @return a context to use in a try-with-resource block
     */
    DocumentContext writingDocument();
}
