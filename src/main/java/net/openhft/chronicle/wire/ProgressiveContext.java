/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

/**
 * Context data that tracks whether it must be written for an output context.
 * <p>
 * Implementations normally keep the last written count in a transient field:
 * <pre>
 * final class SchemaContext extends SelfDescribingMarshallable implements ProgressiveContext {
 *     private String schema;
 *     private transient int lastContextCount = -1;
 *
 *     &#64;Override
 *     public boolean needsResending(int contextCount) {
 *         if (contextCount &lt;= lastContextCount)
 *             return false;
 *         lastContextCount = contextCount;
 *         return true;
 *     }
 * }
 * </pre>
 * Call this method with {@link MarshallableOut#contextCount()} or the equivalent
 * {@link DocumentContext#contextCount()} immediately before writing the context in the same held
 * document.
 */
public interface ProgressiveContext {

    /**
     * Records a strictly higher context count and reports whether this context must be written.
     *
     * @param contextCount count from an open document context
     * @return true only when {@code contextCount} is greater than the previously recorded count
     */
    boolean needsResending(int contextCount);
}
