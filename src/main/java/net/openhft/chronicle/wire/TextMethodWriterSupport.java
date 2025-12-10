/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;

/**
 * Helpers for building text-based method writers that inject interface comments and configure
 * builders consistently for YAML/TEXT wire outputs.
 */
final class TextMethodWriterSupport {
    /**
     * Utility holder; not instantiable.
     */
    private TextMethodWriterSupport() {
    }

    /**
     * Creates an invocation handler for text method writers, emitting any {@link Comment} annotations
     * found on the supplied interfaces before wiring the handler.
     */
    static TextMethodWriterInvocationHandler newInvocationHandler(YamlWireOut<?> wire, Class<?>... interfaces) {
        for (Class<?> anInterface : interfaces) {
            Comment c = Jvm.findAnnotation(anInterface, Comment.class);
            if (c != null)
                wire.writeComment(c.value());
        }
        return new TextMethodWriterInvocationHandler(interfaces[0], wire);
    }

    /**
     * Prepares a builder wired to the supplied {@link YamlWireOut} and {@link WireType}, ensuring
     * the handler uses the text writer invocation handler above.
     */
    static <T> VanillaMethodWriterBuilder<T> builder(YamlWireOut<?> wire, WireType wireType, Class<T> tClass) {
        VanillaMethodWriterBuilder<T> builder = new VanillaMethodWriterBuilder<>(tClass,
                wireType,
                () -> newInvocationHandler(wire, tClass));
        builder.marshallableOut(wire);
        return builder;
    }

    /**
     * Convenience method to build a writer that may implement additional interfaces and ensures the
     * caller switches to text documents for the duration of the call.
     */
    static <T> T writer(YamlWireOut<?> wire, WireType wireType, Runnable useTextDocuments, Class<T> tClass, Class<?>... additional) {
        VanillaMethodWriterBuilder<T> builder = builder(wire, wireType, tClass);
        for (Class<?> aClass : additional)
            builder.addInterface(aClass);
        useTextDocuments.run();
        return builder.build();
    }
}
