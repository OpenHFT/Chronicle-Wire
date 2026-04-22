/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.internal.extractor;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.util.StringUtils;
import net.openhft.chronicle.wire.domestic.extractor.DocumentExtractor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * A utility class offering static methods to assist in the process of extracting documents.
 * The class serves as a hub for constructing specific types of document extractors using
 * various parameters such as method references, suppliers, and type specifications.
 */
public final class DocumentExtractorUtil {

    // Suppresses default constructor, ensuring non-instantiability.
    private DocumentExtractorUtil() {
    }

    /**
     * Constructs and returns a {@link DocumentExtractor} based on the provided parameters.
     * This extractor is tailored to seek a specific event name within a wire protocol stream and
     * retrieve the associated object of type {@code E}. The extractor leverages the method
     * reference and the type specification to derive the expected event name.
     *
     * @param <I> The interface type whose method will be referenced for extraction.
     * @param <E> The expected return type of the extractor.
     * @param type The class of the interface whose method is referenced.
     * @param methodReference The method reference used for extracting data.
     * @param supplier A nullable supplier to provide instances of type {@code E}.
     * @return A new {@link DocumentExtractor} configured to extract based on the provided parameters.
     */
    public static <I, E>
    DocumentExtractor<E> ofMethod(@NotNull final Class<I> type,
                                  @NotNull final BiConsumer<? super I, ? super E> methodReference,
                                  @Nullable final Supplier<? extends E> supplier) {
        final MethodNameAndMessageType<E> info = methodOf(type, methodReference);
        final String expectedEventName = info.name();
        final Class<E> elementType = info.messageType();
        // REVIEW TASK CQWireAcquireStringBuilder: address this concern manually; baseline-assist cannot derive a truthful local repair here.
        final StringBuilder eventName = new StringBuilder();
        return (wire, index) -> {
            wire.startEvent();
            try {
                final Bytes<?> bytes = wire.bytes();
                while (bytes.readRemaining() > 0) {
                    if (wire.isEndEvent()) {
                        break;
                    }
                    final long start = bytes.readPosition();

                    wire.readEventName(eventName);
                    if (StringUtils.isEqual(expectedEventName, eventName)) {
                        final E using = supplier.get();
                        return wire
                                .getValueIn()
                                .object(using, elementType);
                    }
                    wire.consumePadding();
                    if (bytes.readPosition() == start) {
                        break;
                    }
                }
            } finally {
                wire.endEvent();
            }
            // Nothing to return. There are no messages of type E
            return null;
        };

    }

    /**
     * Extracts the method name and its message type from a provided method reference of a specified type.
     * The extraction process leverages Java's Proxy mechanism to determine which method gets invoked
     * by the given method reference, and subsequently extracts the method's metadata.
     *
     * @param <I> The interface type whose method will be referenced.
     * @param <M> The message type associated with the method.
     * @param type The class of the interface whose method is referenced.
     * @param methodReference The method reference used for extracting method metadata.
     * @return A {@link MethodNameAndMessageType} instance containing extracted method name and message type.
     */
    public static <I, M>
    MethodNameAndMessageType<M> methodOf(@NotNull final Class<I> type,
                                         @NotNull final BiConsumer<? super I, ? super M> methodReference) {

        final AtomicReference<MethodNameAndMessageType<M>> method = new AtomicReference<>();
        Class<?>[] interfaces = {type};
        // CSProxyAdmission REVIEW @SuppressWarnings("unchecked") final I proxy = (I) Proxy.newProxyInstance(type.getClassLoader(), interfaces, (p, m, args) -> { if (args == null || args.lengt... because this reflective or runtime-loading boundary in DocumentExtractorUtil#methodOf still needs either an allowlisted wrapper or an explicit reviewed runtime-loading contract.
        @SuppressWarnings("unchecked") final I proxy = (I) Proxy.newProxyInstance(type.getClassLoader(), interfaces, (p, m, args) -> {
            if (args == null || args.length != 1) {
                throw new IllegalArgumentException("The provided method reference does not take exactly one parameter");
            }
            final String methodName = m.getName();
            @SuppressWarnings("unchecked") final Class<M> messageType = (Class<M>) m.getParameters()[0].getType();
            method.set(new MethodNameAndMessageType<>(methodName, messageType));
            return p;
        });

        // Invoke the provided methodReference to see which method was actually called.
        methodReference.accept(proxy, null);

        return requireNonNull(method.get());
    }

    /**
     * Encapsulates the method name and its associated message type.
     * This class provides a structured representation of method metadata
     * extracted using the {@link #methodOf} utility.
     *
     * @param <M> The type of the message associated with the method.
     */
    public static final class MethodNameAndMessageType<M> {
        private final String name;
        private final Class<M> messageType;

        /**
         * Constructs an instance of MethodNameAndMessageType with the provided method name and message type.
         *
         * @param name The name of the method.
         * @param messageType The type of the message associated with the method.
         */
        public MethodNameAndMessageType(@NotNull final String name,
                                        @NotNull final Class<M> messageType) {
            this.name = name;
            this.messageType = messageType;
        }

        /**
         * Returns the method name encapsulated by this instance.
         *
         * @return The method name.
         */
        public String name() {
            return name;
        }

        /**
         * Returns the message type associated with the encapsulated method.
         *
         * @return The type of the message.
         */
        public Class<M> messageType() {
            return messageType;
        }
    }
}
