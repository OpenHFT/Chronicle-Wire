/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.MethodWriterBuilder;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.annotation.DontChain;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Represents a destination to which marshallable messages are written.
 * Typical implementors are queue appenders or {@code WireOut} variants such as
 * file or HTTP writers. Use {@link #builder(URL)} to create a suitable
 * implementation based on a given URL.
 */
@DontChain
public interface MarshallableOut extends DocumentWritten, RollbackIfNotCompleteNotifier {

    /**
     * Creates a builder using the supplied URL. The URL scheme chooses the
     * implementation, for example {@code file:} or {@code http:}.
     *
     * @param url destination URL
     * @return builder for the chosen output type
     */
    static MarshallableOutBuilder builder(URL url) {
        return new MarshallableOutBuilder(url);
    }

    /**
     * Begins a document that completes when the returned context is closed. It
     * should always be used within a try-with-resources block:
     * <pre>
     * try (DocumentContext dc = appender.writingDocument()) {
     *     dc.wire().write("message").text("Hello World");
     * }
     * </pre>
     * Some implementations hold a write lock until {@code close()}, so keep the
     * body brief. Queue appenders may block each other while tailers continue.
     */
    @NotNull
    default DocumentContext writingDocument() throws UnrecoverableTimeoutException {
        return writingDocument(false);
    }

    /**
     * Opens a document for writing and optionally marks it as meta-data. The
     * context must be closed, ideally using try-with-resources. Locking
     * behaviour is the same as {@link #writingDocument()}.
     *
     * @param metaData true to mark the document as meta-data
     * @return the opened {@code DocumentContext}
     * @throws UnrecoverableTimeoutException if the operation times out
     */
    DocumentContext writingDocument(boolean metaData) throws UnrecoverableTimeoutException;

    /**
     * Returns a {@code DocumentContext}, reusing one if the caller already holds
     * it. This is used by chained {@link MethodWriter} calls where the first
     * call opens the context and later calls share it. The context should be
     * closed once all messages are written.
     */
    DocumentContext acquireWritingDocument(boolean metaData) throws UnrecoverableTimeoutException;

    /**
     * @return {@code true} if callers are expected to record the history of each
     * message. This is used for tracing or debugging across systems.
     */
    default boolean recordHistory() {
        return false;
    }

    /**
     * Writes a complete message consisting of a key and value.
     * The value may be a scalar or another marshallable object.
     *
     * @param key   field name for the value
     * @param value data to write
     */
    default void writeMessage(WireKey key, Object value) throws UnrecoverableTimeoutException {
        @NotNull DocumentContext dc = writingDocument();
        try {
            Wire wire = dc.wire();
            wire.write(key).object(value);
        } catch (Throwable t) {
            dc.rollbackOnClose();
            throw Jvm.rethrow(t);
        } finally {
            dc.close();
        }
    }

    /**
     * Writes an event name and value as a self-contained message.
     * The context is closed after the write even if an exception occurs.
     *
     * @param eventName name of the event
     * @param value data to write
     * @throws UnrecoverableTimeoutException if the operation times out
     */
    default void writeMessage(String eventName, Object value) throws UnrecoverableTimeoutException {
        @NotNull DocumentContext dc = writingDocument();
        try {
            Wire wire = dc.wire();
            wire.write(eventName).object(value);
        } catch (Throwable t) {
            dc.rollbackOnClose();
            throw Jvm.rethrow(t);
        } finally {
            dc.close();
        }
    }

    /**
     * Writes the supplied {@link WriteMarshallable} as a complete document. The
     * context is closed after the write even on error.
     *
     * @param writer marshallable to write
     * @throws UnrecoverableTimeoutException if the operation times out
     * @throws InvalidMarshallableException  if serialization fails
     */
    default void writeDocument(@NotNull WriteMarshallable writer) throws UnrecoverableTimeoutException, InvalidMarshallableException {
        try (@NotNull DocumentContext dc = writingDocument(false)) {
            try {
                Wire wire = dc.wire();
                writer.writeMarshallable(wire);
            } catch (Throwable t) {
                dc.rollbackOnClose();
                throw Jvm.rethrow(t);
            }
        }
    }

    /**
     * Serialises an object that implements {@link WriteBytesMarshallable} using
     * its own byte-level format. The context is closed after the write.
     *
     * @param marshallable object responsible for writing its bytes
     * @throws UnrecoverableTimeoutException if the operation times out
     * @throws InvalidMarshallableException  if serialisation fails
     */
    default void writeBytes(@NotNull WriteBytesMarshallable marshallable) throws UnrecoverableTimeoutException, InvalidMarshallableException {
        @NotNull DocumentContext dc = writingDocument();
        try {
            marshallable.writeMarshallable(dc.wire().bytes());
        } catch (Throwable t) {
            dc.rollbackOnClose();
            throw Jvm.rethrow(t);
        } finally {
            dc.close();
        }
    }

    /**
     * Writes an object using a caller supplied lambda to perform the
     * serialisation. Useful for types that are not {@link WriteMarshallable}.
     * Example:
     * <pre>
     * out.writeDocument(o, (v, obj) -> v.text(obj.toString()));
     * </pre>
     *
     * @param t      object to be serialised
     * @param writer callback that writes the object to the {@link ValueOut}
     * @param <T>    object type
     * @throws UnrecoverableTimeoutException if the operation times out
     * @throws InvalidMarshallableException  if the callback fails
     */
    default <T> void writeDocument(T t, @NotNull BiConsumer<ValueOut, T> writer) throws UnrecoverableTimeoutException, InvalidMarshallableException {
        @NotNull DocumentContext dc = writingDocument();
        try {
            Wire wire = dc.wire();
            writer.accept(wire.getValueOut(), t);
        } catch (Throwable e) {
            dc.rollbackOnClose();
            throw Jvm.rethrow(e);
        } finally {
            dc.close();
        }
    }

    /**
     * Writes the supplied text as a single document.
     *
     * @param text the text to write
     * @throws UnrecoverableTimeoutException if the operation times out
     */
    default void writeText(@NotNull CharSequence text) throws UnrecoverableTimeoutException {
        @NotNull DocumentContext dc = writingDocument();
        try {
            dc.wire().getValueOut().text(text);
        } catch (Throwable t) {
            dc.rollbackOnClose();
            throw Jvm.rethrow(t);
        } finally {
            dc.close();
        }
    }

    /**
     * Writes the given map as key/value pairs within a document.
     *
     * @param map map to serialise
     * @throws UnrecoverableTimeoutException if the operation times out
     */
    default void writeMap(@NotNull Map<?, ?> map) throws UnrecoverableTimeoutException {
        @NotNull DocumentContext dc = writingDocument();
        try {
            Wire wire = dc.wire();
            for (@NotNull Map.Entry<?, ?> entry : map.entrySet()) {
                wire.writeEvent(Object.class, entry.getKey())
                        .object(Object.class, entry.getValue());
            }
        } catch (Throwable t) {
            dc.rollbackOnClose();
            throw Jvm.rethrow(t);
        } finally {
            dc.close();
        }
    }

    /**
     * Creates a dynamic proxy for the supplied interfaces. Invoking a method on
     * the proxy serialises that call to this {@code MarshallableOut}.
     * The returned proxy always implements {@code tClass}; optional extra
     * interfaces must be cast before use.
     *
     * @param tClass     primary interface
     * @param additional further interfaces
     * @return proxy implementing the given interfaces
     */
    @SuppressWarnings("rawtypes")
    @NotNull
    default <T> T methodWriter(@NotNull Class<T> tClass, Class<?>... additional) {
        VanillaMethodWriterBuilder<T> builder =
                (VanillaMethodWriterBuilder<T>) methodWriterBuilder(false, tClass);
        Stream.of(additional).forEach(builder::addInterface);
        return builder.build();
    }

    /**
     * Convenience method returning a {@link MethodWriterBuilder} for the given
     * interface. All method calls made via the built proxy are written to this
     * output.
     *
     * @param tClass primary interface
     * @return configured builder
     */
    @NotNull
    default <T> MethodWriterBuilder<T> methodWriterBuilder(@NotNull Class<T> tClass) {
        return methodWriterBuilder(false, tClass);
    }

    /**
     * Returns a builder for a method writer proxy with optional metadata
     * recording. The builder exposes further configuration such as interceptors
     * and wire type before {@code build()} is invoked.
     *
     * @param metaData write each call as metadata if true
     * @param tClass   primary interface
     * @return configurable builder
     */
    @NotNull
    default <T> MethodWriterBuilder<T> methodWriterBuilder(boolean metaData, @NotNull Class<T> tClass) {
        // Creates a new builder instance with the specified WireType and InvocationHandler
        VanillaMethodWriterBuilder<T> builder = new VanillaMethodWriterBuilder<>(tClass,
                WireType.BINARY_LIGHT,
                () -> new BinaryMethodWriterInvocationHandler(tClass, metaData, this));

        // Configure the builder
        builder.marshallableOut(this);
        builder.metaData(metaData);

        // If the current instance can be closed, set its close behavior
        if (this instanceof Closeable)
            builder.onClose((Closeable) this);
        return builder;
    }
}
