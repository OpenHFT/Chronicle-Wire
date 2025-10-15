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

import net.openhft.chronicle.bytes.MethodWriterInvocationHandler;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * {@link MethodWriterInvocationHandler} that serialises method calls to a
 * {@link MarshallableOut} using the binary wire format.
 * <p>
 * Each call is wrapped in a {@link WriteDocumentContext} which is opened and
 * closed by this handler. Calls may be written as data or metadata documents
 * depending on the {@link #metaData} flag.
 */
public class BinaryMethodWriterInvocationHandler extends AbstractMethodWriterInvocationHandler {

    /** Supplier for the target {@link MarshallableOut} used to serialise method calls. */
    @NotNull
    private final Supplier<MarshallableOut> marshallableOutSupplier;

    /** Flag to determine if metadata should be written. */
    private final boolean metaData;

    /**
     * Create a handler bound to a single {@link MarshallableOut} instance.
     *
     * @param tClass           The class type associated with this handler.
     * @param metaData         Flag to determine if metadata is to be written.
     * @param marshallableOut  The MarshallableOut instance for binary writing.
     */
    BinaryMethodWriterInvocationHandler(Class<?> tClass, final boolean metaData, @NotNull MarshallableOut marshallableOut) {
        this(tClass, metaData, () -> marshallableOut);
    }

    /**
     * Create a handler using the given class, metadata flag and a supplier of MarshallableOut.
     *
     * @param tClass                  primary interface of the proxy
     * @param metaData                Flag to determine if metadata is to be written.
     * @param marshallableOutSupplier supplier of the destination for serialised calls
     */
    public BinaryMethodWriterInvocationHandler(Class<?> tClass, final boolean metaData, Supplier<MarshallableOut> marshallableOutSupplier) {
        super(tClass);
        this.marshallableOutSupplier = marshallableOutSupplier;
        this.metaData = metaData;
        recordHistory = marshallableOutSupplier.get().recordHistory();
    }

    /**
     * Intercepts calls on the proxy. A call to {@code writingDocument()} is handled here
     * to expose the underlying {@link WriteDocumentContext} directly.
     * Other calls are delegated to {@link AbstractMethodWriterInvocationHandler#doInvoke(Object, Method, Object[])}.
     */
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

    /**
     * Serialises the given {@code method} call with {@code args} to the configured output.
     * Each invocation acquires a new {@link WriteDocumentContext} which is marked
     * to possibly hold metadata according to {@link #metaData}.
     * If the return type of the method is an interface the document context is marked as
     * {@code chained} so subsequent calls may share the same context.
     */
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
