/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
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
 * This interface defines methods for outputting Marshallable objects. It provides various methods for
 * writing different types of data, including documents, messages, and values.
 */
@DontChain
public interface MarshallableOut extends DocumentWritten {

    /**
     * Returns a new MarshallableOutBuilder for the specified URL.
     *
     * @param url the URL to be used by the builder
     * @return a new instance of MarshallableOutBuilder
     */
    static MarshallableOutBuilder builder(URL url) {
        return new MarshallableOutBuilder(url);
    }

    /**
     * Start a document which is completed when DocumentContext.close() is called. You can use a
     * <pre>
     * try(DocumentContext dc = appender.writingDocument()) {
     *      dc.wire().write("message").text("Hello World");
     * }
     * </pre>
     * <p>
     * WARNING : any data written inside the writingDocument(),  should be performed as quickly as
     * possible  because a write lock is held  until the DocumentContext is closed by the
     * try-with-resources,  this blocks other appenders and tailers.
     * <pre>
     * try (DocumentContext dc = appender.writingDocument()) {
     *      // this should be performed as quickly as possible because a write lock is held until
     * the
     *      // DocumentContext is closed by the try-with-resources,  this blocks other appenders
     *      and tailers.
     * }
     * </pre>
     * @return a new DocumentContext
     * @throws UnrecoverableTimeoutException if the operation times out
     */
    @NotNull
    default DocumentContext writingDocument() throws UnrecoverableTimeoutException {
        return writingDocument(false);
    }

    /**
     * Begins writing a new DocumentContext. This method always requires a close() call when done.
     *
     * @param metaData metadata to be associated with the document
     * @return a new DocumentContext
     * @throws UnrecoverableTimeoutException if the operation times out
     */
    DocumentContext writingDocument(boolean metaData) throws UnrecoverableTimeoutException;

    /**
     * Begins writing a new DocumentContext or reuses an existing one. It's optional to call close() when done.
     *
     * @param metaData metadata to be associated with the document
     * @return an existing or a new DocumentContext
     * @throws UnrecoverableTimeoutException if the operation times out
     */
    DocumentContext acquireWritingDocument(boolean metaData) throws UnrecoverableTimeoutException;

    /**
     * Checks if this output is configured to record the history of the message.
     *
     * @return true if history recording is enabled, false otherwise
     */
    default boolean recordHistory() {
        return false;
    }

    /**
     * Writes a key-value pair, which could be a scalar or a Marshallable object, as a message.
     *
     * @param key   the key to be written
     * @param value the value to be written with the key
     * @throws UnrecoverableTimeoutException if the operation times out
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
     * Writes an event name and its corresponding value as a message.
     *
     * @param eventName the name of the event to be written
     * @param value     the value to be written with the event name
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
     * Writes the given Marshallable as a document or message.
     *
     * @param writer the Marshallable to write
     * @throws UnrecoverableTimeoutException  if the operation times out
     * @throws InvalidMarshallableException if the provided Marshallable is invalid
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
     * @param marshallable to write to excerpt.
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
     * Write an object with a custom marshalling.
     *
     * @param t      to write
     * @param writer using this code
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
     * @param text to write a message
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
     * Write a Map as a marshallable
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
     * Proxy an interface so each message called is written for replay.
     *
     * @param tClass     primary interface
     * @param additional any additional interfaces
     * @return a proxy which implements the primary interface (additional interfaces have to be
     * cast)
     */
    @SuppressWarnings("rawtypes")
    @NotNull
    default <T> T methodWriter(@NotNull Class<T> tClass, Class... additional) {
        return methodWriter(false, tClass, additional);
    }

    /**
     * Proxy an interface so each message called is written for method.
     *
     * @param metaData   true if you wish to write every method as meta data
     * @param tClass     primary interface
     * @param additional any additional interfaces
     * @return a proxy which implements the primary interface (additional interfaces have to be
     * cast)
     * @deprecated use methodWriterBuilder with Stream.of(additional).forEach(builder::addInterface);
     */
    @Deprecated(/* to be removed in x.25 */)
    @SuppressWarnings({"rawtypes", "unchecked"})
    @NotNull
    default <T> T methodWriter(boolean metaData, @NotNull Class<T> tClass, Class... additional) {
        VanillaMethodWriterBuilder<T> builder =
                (VanillaMethodWriterBuilder<T>) methodWriterBuilder(metaData, tClass);
        Stream.of(additional).forEach(builder::addInterface);
        return builder.build();
    }

    /**
     * Return a builder for a proxy an interface so each message called is written for method.
     *
     * @param tClass primary interface
     * @return a builder for a proxy which implements the interface
     */
    @NotNull
    default <T> MethodWriterBuilder<T> methodWriterBuilder(@NotNull Class<T> tClass) {
        return methodWriterBuilder(false, tClass);
    }

    /**
     * Proxy an interface so each message called is written to a file for method.
     *
     * @param metaData true if you wish to write every method as meta data
     * @param tClass   primary interface
     * @return a proxy which implements the interface
     */
    @NotNull
    default <T> MethodWriterBuilder<T> methodWriterBuilder(boolean metaData, @NotNull Class<T> tClass) {
        VanillaMethodWriterBuilder<T> builder = new VanillaMethodWriterBuilder<>(tClass,
                WireType.BINARY_LIGHT,
                () -> new BinaryMethodWriterInvocationHandler(tClass, metaData, this));
        builder.marshallableOut(this);
        builder.metaData(metaData);
        if (this instanceof Closeable)
            builder.onClose((Closeable) this);
        return builder;
    }
}
