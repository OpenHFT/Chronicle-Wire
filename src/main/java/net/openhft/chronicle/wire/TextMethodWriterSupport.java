/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.Comment;

final class TextMethodWriterSupport {
    private TextMethodWriterSupport() {
    }

    static TextMethodWriterInvocationHandler newInvocationHandler(YamlWireOut<?> wire, Class<?>... interfaces) {
        for (Class<?> anInterface : interfaces) {
            Comment c = Jvm.findAnnotation(anInterface, Comment.class);
            if (c != null)
                wire.writeComment(c.value());
        }
        return new TextMethodWriterInvocationHandler(interfaces[0], wire);
    }

    static <T> VanillaMethodWriterBuilder<T> builder(YamlWireOut<?> wire, WireType wireType, Class<T> tClass) {
        VanillaMethodWriterBuilder<T> builder = new VanillaMethodWriterBuilder<>(tClass,
                wireType,
                () -> newInvocationHandler(wire, tClass));
        builder.marshallableOut(wire);
        return builder;
    }

    static <T> T writer(YamlWireOut<?> wire, WireType wireType, Runnable useTextDocuments, Class<T> tClass, Class<?>... additional) {
        VanillaMethodWriterBuilder<T> builder = builder(wire, wireType, tClass);
        for (Class<?> aClass : additional)
            builder.addInterface(aClass);
        useTextDocuments.run();
        return builder.build();
    }
}
