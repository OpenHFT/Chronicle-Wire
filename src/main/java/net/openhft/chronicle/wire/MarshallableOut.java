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
 *
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
     *
     * @return the DocumentContext
     */
    DocumentContext writingDocument();

    /**
     * @return true is this output is configured to expect the history of the message to be written to.
     */
    boolean recordHistory();

    /**
     * Wrie a key and value which could be a scalar or a marshallable.
     *
     * @param key   to write
     * @param value to write with it.
     */
    default void writeMessage(WireKey key, Object value) {
        try (DocumentContext dc = writingDocument()) {
            dc.wire().write(key).object(value);
        }
    }

    /**
     * Write the Marshallable as a document/message
     *
     * @param writer to write
     */
    default void writeDocument(WriteMarshallable writer) {
        try (DocumentContext dc = writingDocument()) {
            writer.writeMarshallable(dc.wire());
        }
    }

    /**
     * @param marshallable to write to excerpt.
     */
    default void writeBytes(@NotNull WriteBytesMarshallable marshallable) {
        try (DocumentContext dc = writingDocument()) {
            marshallable.writeMarshallable(dc.wire().bytes());
        }
    }

    /**
     * Write an object with a custom marshalling.
     *
     * @param t      to write
     * @param writer using this code
     */
    default <T> void writeDocument(T t, BiConsumer<ValueOut, T> writer) {
        try (DocumentContext dc = writingDocument()) {
            writer.accept(dc.wire().getValueOut(), t);
        }
    }

    /**
     * @param text to write a message
     */
    default void writeText(CharSequence text) {
        try (DocumentContext dc = writingDocument()) {
            dc.wire().bytes().append8bit(text);
        }
    }

    /**
     * Write a Map as a marshallable
     */
    default void writeMap(Map<?, ?> map) {
        try (DocumentContext dc = writingDocument()) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                dc.wire().writeEvent(Object.class, entry.getKey()).object(Object.class, entry.getValue());
            }
        }
    }

    /**
     * Proxy an interface so each message called is written to a file for replay.
     *
     * @param tClass     primary interface
     * @param additional any additional interfaces
     * @return a proxy which implements the primary interface (additional interfaces have to be cast)
     */
    default <T> T methodWriter(Class<T> tClass, Class... additional) {
        Class[] interfaces = ObjectUtils.addAll(tClass, additional);

        //noinspection unchecked
        return (T) Proxy.newProxyInstance(tClass.getClassLoader(), interfaces, new MethodWriterInvocationHandler(this));
    }

    default <T> MethodWriterBuilder<T> methodWriterBuilder(Class<T> tClass) {
        return new MethodWriterBuilder<>(this, tClass);
    }
}
