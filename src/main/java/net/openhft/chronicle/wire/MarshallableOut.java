/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Anything you can write Marshallable objects to.
 */
@DontChain
public interface MarshallableOut extends DocumentWritten {
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
     */
    @NotNull
    default DocumentContext writingDocument() throws UnrecoverableTimeoutException {
        return writingDocument(false);
    }

    /**
     * Start a new DocumentContext, must always call close() when done.
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
     * Wrie a key and value which could be a scalar or a marshallable.
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
     * Write the Marshallable as a document/message
     *
     * @param writer to write
     */
    default void writeDocument(@NotNull WriteMarshallable writer) throws UnrecoverableTimeoutException {
        try (@NotNull DocumentContext dc = writingDocument(false)) {
            Wire wire = dc.wire();
            writer.writeMarshallable(wire);
        }
    }

    /**
     * @param marshallable to write to excerpt.
     */
    default void writeBytes(@NotNull WriteBytesMarshallable marshallable) throws UnrecoverableTimeoutException {
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
    default <T> void writeDocument(T t, @NotNull BiConsumer<ValueOut, T> writer) throws UnrecoverableTimeoutException {
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
     * Proxy an interface so each message called is written to a file for replay.
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
     * Proxy an interface so each message called is written to a file for method.
     *
     * @param metaData   true if you wish to write every method as meta data
     * @param tClass     primary interface
     * @param additional any additional interfaces
     * @return a proxy which implements the primary interface (additional interfaces have to be
     * cast)
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @NotNull
    default <T> T methodWriter(boolean metaData, @NotNull Class<T> tClass, Class... additional) {
        VanillaMethodWriterBuilder<T> builder = new VanillaMethodWriterBuilder<>(tClass,
                WireType.BINARY_LIGHT,
                () -> new BinaryMethodWriterInvocationHandler(metaData, this));
        Stream.of(additional).forEach(builder::addInterface);

        builder.marshallableOut(this);
        builder.metaData(metaData);
        return builder.build();
    }

    @NotNull
    default <T> MethodWriterBuilder<T> methodWriterBuilder(@NotNull Class<T> tClass) {
        VanillaMethodWriterBuilder<T> builder = new VanillaMethodWriterBuilder<>(tClass,
                WireType.BINARY_LIGHT,
                () -> new BinaryMethodWriterInvocationHandler(false, this));
        builder.marshallableOut(this);
        return builder;
    }
}
