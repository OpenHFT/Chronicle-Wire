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

import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * The BinaryMethodWriterInvocationHandler handles method invocations specific to binary writing scenarios with optional metadata support.
 */
public class BinaryMethodWriterInvocationHandler extends AbstractMethodWriterInvocationHandler {
    @NotNull
    private final Supplier<MarshallableOut> marshallableOutSupplier;
    private final boolean metaData;  // Flag to determine if metadata should be written.

    /**
     * Constructor that initializes the handler using the given class, metadata flag and MarshallableOut.
     * This constructor uses a constant MarshallableOut.
     *
     * @param tClass           The class type associated with this handler.
     * @param metaData         Flag to determine if metadata is to be written.
     * @param marshallableOut  The MarshallableOut instance for binary writing.
     */
    BinaryMethodWriterInvocationHandler(Class<?> tClass, final boolean metaData, @NotNull MarshallableOut marshallableOut) {
        this(tClass, metaData, () -> marshallableOut);
    }

    /**
     * Constructor that initializes the handler using the given class, metadata flag and a supplier of MarshallableOut.
     *
     * @param tClass                  The class type associated with this handler.
     * @param metaData                Flag to determine if metadata is to be written.
     * @param marshallableOutSupplier The supplier providing instances of MarshallableOut.
     */
    public BinaryMethodWriterInvocationHandler(Class<?> tClass, final boolean metaData, Supplier<MarshallableOut> marshallableOutSupplier) {
        super(tClass);
        this.marshallableOutSupplier = marshallableOutSupplier;
        this.metaData = metaData;
        recordHistory = marshallableOutSupplier.get().recordHistory();
    }

    @Override
    protected Object doInvoke(Object proxy, Method method, Object[] args) {
        if (method.getName().equals("writingDocument") && method.getParameterCount() == 0) {
            MarshallableOut marshallableOut = this.marshallableOutSupplier.get();
            return marshallableOut.writingDocument(metaData);
        }
        return super.doInvoke(proxy, method, args);
    }

    /**
     * Gets the current metadata setting for this handler.
     *
     * @return {@code true} if metadata is enabled, {@code false} otherwise.
     */
    public boolean metaData() {
        return metaData;
    }

    @Override
    protected void handleInvoke(Method method, Object[] args) {
        boolean chained = method.getReturnType().isInterface();
        MarshallableOut marshallableOut = this.marshallableOutSupplier.get();
        try (WriteDocumentContext dc = (WriteDocumentContext) marshallableOut.acquireWritingDocument(metaData)) {
            try {
                dc.chainedElement(chained);
                Wire wire = dc.wire();
                handleInvoke(method, args, wire);
                wire.padToCacheAlign();

            } catch (Throwable t) {
                dc.rollbackOnClose();
                throw Jvm.rethrow(t);
            }
        }
    }
}
