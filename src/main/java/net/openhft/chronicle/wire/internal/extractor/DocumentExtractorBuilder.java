/*
 * Copyright 2016-2022 chronicle.software
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

package net.openhft.chronicle.wire.internal.extractor;

import net.openhft.chronicle.core.util.ThreadConfinementAsserter;
import net.openhft.chronicle.wire.domestic.extractor.DocumentExtractor;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * Responsible for building and configuring document extractors for a specified type {@code E}.
 * This class provides flexibility in the extraction process, enabling efficient document extraction.
 *
 * @since 2023-09-15
 */
public final class DocumentExtractorBuilder<E> implements DocumentExtractor.Builder<E> {

    // Represents the type of element to be extracted.
    private final Class<E> elementType;

    // A supplier to provide instances of the type E.
    private Supplier<? extends E> supplier;

    // Flag to determine if reuse of the supplier is confined to the current thread.
    private boolean threadConfinedReuse;

    // Reference to the method used for extraction.
    private MethodRef<Object, E> methodRef;

    /**
     * Constructs a new DocumentExtractorBuilder for the specified element type.
     *
     * @param elementType Class of the elements to be extracted.
     */
    public DocumentExtractorBuilder(@NotNull final Class<E> elementType) {
        this.elementType = requireNonNull(elementType);
    }

    @NotNull
    @Override
    public DocumentExtractor.Builder<E> withReusing(@NotNull Supplier<? extends E> supplier) {
        this.supplier = requireNonNull(supplier);
        return this;
    }

    @NotNull
    @Override
    public DocumentExtractor.Builder<E> withThreadConfinedReuse() {
        threadConfinedReuse = true;
        return this;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <I> DocumentExtractor.Builder<E> withMethod(@NotNull final Class<I> interfaceType,
                                                       @NotNull final BiConsumer<? super I, ? super E> methodReference) {
        methodRef = (MethodRef<Object, E>) new MethodRef<>(interfaceType, methodReference);
        return this;
    }

    @NotNull
    @Override
    public DocumentExtractor<E> build() {

        if (methodRef != null) {
            if (supplier == null) {
                // () -> null means null will be used as reuse meaning new objects are created
                return DocumentExtractorUtil.ofMethod(methodRef.interfaceType(), methodRef.methodReference(), () -> null);
            }
            return DocumentExtractorUtil.ofMethod(methodRef.interfaceType(), methodRef.methodReference(), guardedSupplier());
        }

        if (supplier == null) {
            return (wire, index) -> wire
                    .getValueIn()
                    .object(elementType); // No lambda capture
        } else {
            final Supplier<? extends E> internalSupplier = guardedSupplier();
            return (wire, index) -> {
                final E using = internalSupplier.get();
                return wire
                        .getValueIn()
                        .object(using, elementType); // Lambda capture
            };
        }
    }

    /**
     * Provides a thread-safe supplier, ensuring the supplier's reuse is either confined to the
     * current thread or utilizes a thread-local mechanism.
     *
     * @return A guarded supplier of type E.
     */
    Supplier<E> guardedSupplier() {
        // Determines which supplier to use based on thread confinement.
        return threadConfinedReuse
                ? new ThreadConfinedSupplier<>(supplier)
                : new ThreadLocalSupplier<>(supplier);
    }

    /**
     * A supplier backed by a ThreadLocal instance, ensuring separate values for each thread.
     */
    static final class ThreadLocalSupplier<E> implements Supplier<E> {

        // ThreadLocal instance to provide thread-specific values.
        private final ThreadLocal<E> threadLocal;

        /**
         * Constructs the ThreadLocalSupplier with a given supplier.
         *
         * @param supplier The supplier to initialize the thread-local with.
         */
        public ThreadLocalSupplier(@NotNull final Supplier<? extends E> supplier) {
            this.threadLocal = ThreadLocal.withInitial(supplier);
        }

        @Override
        public E get() {
            return threadLocal.get();
        }
    }

    /**
     * A supplier that ensures its use is confined to a single thread, providing thread safety.
     */
    static final class ThreadConfinedSupplier<E> implements Supplier<E> {

        // Utility to assert that the current thread is the one this supplier is confined to.
        private final ThreadConfinementAsserter asserter = ThreadConfinementAsserter.createEnabled();

        // The actual object to be supplied.
        private final E delegate;

        /**
         * Constructs the ThreadConfinedSupplier with a given supplier.
         *
         * @param supplier The supplier to provide the delegate object.
         */
        public ThreadConfinedSupplier(@NotNull final Supplier<? extends E> supplier) {
            // Eagerly create the reuse object
            this.delegate = requireNonNull(supplier.get());
        }

        @Override
        public E get() {
            asserter.assertThreadConfined();
            return delegate;
        }
    }

    /**
     * Represents a reference to a method for extracting data.
     */
    private static final class MethodRef<I, E> {

        // Type of interface the method belongs to.
        final Class<I> interfaceType;

        // Reference to the extraction method.
        final BiConsumer<I, E> methodReference;

        /**
         * Constructs the MethodRef with specified interface type and method reference.
         *
         * @param interfaceType The class type of the interface.
         * @param methodReference The actual method reference for extraction.
         */
        @SuppressWarnings("unchecked")
        public MethodRef(@NotNull final Class<I> interfaceType,
                         @NotNull final BiConsumer<? super I, ? super E> methodReference) {
            this.interfaceType = interfaceType;
            this.methodReference = (BiConsumer<I, E>) methodReference;
        }

        public Class<I> interfaceType() {
            return interfaceType;
        }

        public BiConsumer<I, E> methodReference() {
            return methodReference;
        }
    }
}
