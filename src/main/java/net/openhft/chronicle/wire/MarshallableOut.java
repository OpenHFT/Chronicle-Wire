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
 * Defines the contract for objects that can write out Marshallable objects.
 * Implementing classes or interfaces should provide concrete means to serialize and write Marshallable objects.
 * <p>
 * Use the {@link #builder(URL)} method to create an instance of {@link MarshallableOutBuilder} to help construct
 * appropriate implementations based on provided URLs.
 * </p>
 * @since 2023-09-12
 */
@DontChain
public interface MarshallableOut extends DocumentWritten, RollbackIfNotCompleteNotifier {

    /**
     * Creates and returns a new instance of {@link MarshallableOutBuilder} initialized with the provided URL.
     *
     * @param url The URL which will dictate the specific type of {@code MarshallableOut} to create.
     * @return A new instance of {@code MarshallableOutBuilder}.
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
     * WARNING : any data written inside the writingDocument(), should be performed as quickly as
     * possible because a write lock is held until the DocumentContext is closed by the
     * try-with-resources.
     * For thread safe implementation such as Queue this blocks other appenders. Tailers are never blocked.
     * <pre>
     * try (DocumentContext dc = appender.writingDocument()) {
     *      // this should be performed as quickly as possible for implementations that support cocurrent writers
     * }
     * </pre>
     */
    @NotNull
    default DocumentContext writingDocument() throws UnrecoverableTimeoutException {
        return writingDocument(false);
    }

    /**
     * Begins a new document-writing session with the option to include meta-data.
     * It is crucial to always close the returned {@link DocumentContext} once done writing.
     *
     * @param metaData Indicates if meta-data should be included in the document.
     * @return A new instance of {@code DocumentContext}.
     * @throws UnrecoverableTimeoutException if the operation times out in an unrecoverable manner.
     */
    DocumentContext writingDocument(boolean metaData) throws UnrecoverableTimeoutException;

    /**
     * Start or reuse an existing a DocumentContext, optionally call close() when done.
     */
    DocumentContext acquireWritingDocument(boolean metaData) throws UnrecoverableTimeoutException;

    /**
     * @return true if this output is configured to expect the history of the message to be written
     * to.
     */
    default boolean recordHistory() {
        return false;
    }

    /**
     * Write a key and value which could be a scalar or a marshallable.
     *
     * @param key   to write
     * @param value to write with it.
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
     * Writes a message with a specified event name and associated object value.
     * The method manages the lifecycle of the {@link DocumentContext}, ensuring it's closed after the
     * message is written or if any exception occurs.
     *
     * @param eventName The name of the event associated with the message.
     * @param value The object value to be written as part of the message.
     * @throws UnrecoverableTimeoutException if the operation times out in an unrecoverable manner.
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
     * Writes a Marshallable object as a document or message. This method manages the lifecycle of the
     * {@link DocumentContext}, ensuring it's closed after the document is written or if any exception occurs.
     *
     * @param writer An instance of {@code WriteMarshallable} that knows how to serialize the object.
     * @throws UnrecoverableTimeoutException if the operation times out in an unrecoverable manner.
     * @throws InvalidMarshallableException if the object cannot be serialized properly.
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
     * Serializes a Marshallable object directly to bytes. This method manages the lifecycle of the
     * {@link DocumentContext}, ensuring it's closed after the serialization is done or if any exception occurs.
     *
     * @param marshallable An instance of {@code WriteBytesMarshallable} that knows how to serialize the object to bytes.
     * @throws UnrecoverableTimeoutException if the operation times out in an unrecoverable manner.
     * @throws InvalidMarshallableException if the object cannot be serialized properly.
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
     * Writes a provided object using a custom marshalling mechanism specified by a {@link BiConsumer}.
     * This method allows clients to have custom serialization logic for any object.
     * The method also manages the lifecycle of the {@link DocumentContext}, ensuring it's closed after the object is
     * written or if any exception occurs.
     *
     * @param t      The object to be serialized.
     * @param writer A {@code BiConsumer} that contains the custom serialization logic.
     * @param <T>    The type of the object to be serialized.
     * @throws UnrecoverableTimeoutException if the operation times out in an unrecoverable manner.
     * @throws InvalidMarshallableException if the object cannot be serialized properly.
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
     * Writes a provided text message into the underlying {@link DocumentContext}. The method manages the lifecycle
     * of the context, ensuring it's closed after the text is written or if any exception occurs.
     *
     * @param text The text message to be written.
     * @throws UnrecoverableTimeoutException if the operation times out in an unrecoverable manner.
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
     * Writes a provided map into the underlying {@link DocumentContext} as a marshallable object. Each key-value
     * pair in the map is serialized individually. The method manages the lifecycle of the context, ensuring it's
     * closed after the map is written or if any exception occurs.
     *
     * @param map The map to be serialized and written.
     * @throws UnrecoverableTimeoutException if the operation times out in an unrecoverable manner.
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
     * Creates a proxy of the specified interfaces, where each method call on the proxy is written for replay.
     * The primary interface is always implemented by the proxy, whereas any additional interfaces have to be cast
     * to be accessed on the proxy.
     *
     * @param tClass     primary interface
     * @param additional any additional interfaces
     * @return a proxy which implements the primary interface (additional interfaces have to be
     * cast)
     */
    @SuppressWarnings("rawtypes")
    @NotNull
    default <T> T methodWriter(@NotNull Class<T> tClass, Class... additional) {
        VanillaMethodWriterBuilder<T> builder =
                (VanillaMethodWriterBuilder<T>) methodWriterBuilder(false, tClass);
        Stream.of(additional).forEach(builder::addInterface);
        return builder.build();
    }

    /**
     * Returns a {@code MethodWriterBuilder} that can be used to create a proxy for an interface.
     * Each message called on the proxy will be written for replay. This is a convenience method
     * that assumes metadata is not required.
     *
     * @param tClass The primary interface that the builder will cater to.
     * @return A {@code MethodWriterBuilder} tailored for the given interface class.
     */
    @NotNull
    default <T> MethodWriterBuilder<T> methodWriterBuilder(@NotNull Class<T> tClass) {
        return methodWriterBuilder(false, tClass);
    }

    /**
     * Returns a {@code MethodWriterBuilder} that can be used to create a proxy for an interface.
     * Depending on the {@code metaData} parameter, every method may be written as metadata. Each
     * message called on the proxy will be written to a file for method replay.
     *
     * @param metaData If set to true, every method call will be written as metadata.
     * @param tClass   The primary interface that the builder will cater to.
     * @return A {@code MethodWriterBuilder} tailored for the given interface class and metadata preference.
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
