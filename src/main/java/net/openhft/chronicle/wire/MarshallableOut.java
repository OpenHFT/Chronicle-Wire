/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.WriteBytesMarshallable;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Created by peter.lawrey on 06/02/2016.
 * <p>
 * Anything you can write Marshallable objects to.
 */
public interface MarshallableOut {
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
    DocumentContext writingDocument() throws UnrecoverableTimeoutException;

    /**
     * @return true is this output is configured to expect the history of the message to be written
     * to.
     */
    boolean recordHistory();

    /**
     * Wrie a key and value which could be a scalar or a marshallable.
     *
     * @param key   to write
     * @param value to write with it.
     */
    default void writeMessage(WireKey key, Object value) throws UnrecoverableTimeoutException {
        try (@NotNull DocumentContext dc = writingDocument()) {
            Wire wire = dc.wire();
            wire.write(key).object(value);
        }
    }

    /**
     * Write the Marshallable as a document/message
     *
     * @param writer to write
     */
    default void writeDocument(@NotNull WriteMarshallable writer) throws UnrecoverableTimeoutException {
        try (@NotNull DocumentContext dc = writingDocument()) {
            Wire wire = dc.wire();
            writer.writeMarshallable(wire);
        }
    }

    /**
     * @param marshallable to write to excerpt.
     */
    default void writeBytes(@NotNull WriteBytesMarshallable marshallable) throws UnrecoverableTimeoutException {
        try (@NotNull DocumentContext dc = writingDocument()) {
            marshallable.writeMarshallable(dc.wire().bytes());
        }
    }

    /**
     * Write an object with a custom marshalling.
     *
     * @param t      to write
     * @param writer using this code
     */
    default <T> void writeDocument(T t, @NotNull BiConsumer<ValueOut, T> writer) throws UnrecoverableTimeoutException {
        try (@NotNull DocumentContext dc = writingDocument()) {
            Wire wire = dc.wire();
            writer.accept(wire.getValueOut(), t);
        }
    }

    /**
     * @param text to write a message
     */
    default void writeText(@NotNull CharSequence text) throws UnrecoverableTimeoutException {
        try (@NotNull DocumentContext dc = writingDocument()) {
            dc.wire().bytes().append8bit(text);
        }
    }

    /**
     * Write a Map as a marshallable
     */
    default void writeMap(@NotNull Map<?, ?> map) throws UnrecoverableTimeoutException {
        try (@NotNull DocumentContext dc = writingDocument()) {
            Wire wire = dc.wire();
            for (@NotNull Map.Entry<?, ?> entry : map.entrySet()) {
                wire.writeEvent(Object.class, entry.getKey())
                        .object(Object.class, entry.getValue());
            }
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
    @NotNull
    default <T> T methodWriter(@NotNull Class<T> tClass, Class... additional) {
        Class[] interfaces = ObjectUtils.addAll(tClass, additional);

        //noinspection unchecked
        return (T) Proxy.newProxyInstance(tClass.getClassLoader(), interfaces, new MethodWriterInvocationHandler(this));
    }

    @NotNull
    default <T> MethodWriterBuilder<T> methodWriterBuilder(Class<T> tClass) {
        return new MethodWriterBuilder<>(this, tClass);
    }

    /**
     * adds padding to align to cache lines. So that wire header messages don't straggle cache lines
     *
     * @return the state of padding
     */
    @NotNull
    default Padding padToCacheAlign() {
        return Padding.NEVER;
    }

    enum Padding {
        ALWAYS("always pads to cache lines"),
        NEVER("never adds padding"),
        SMART("adds padding to ensure new wire headers dont straggle cache lines, where " +
                "possible to " +
                "do so " +
                "without " +
                "breaking the" +
                "existing " +
                "message format specification");

        Padding(String comment) {

        }
    }
}
