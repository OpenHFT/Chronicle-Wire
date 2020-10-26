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

import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.function.Supplier;

public class BinaryMethodWriterInvocationHandler extends AbstractMethodWriterInvocationHandler {
    @NotNull
    private final Supplier<MarshallableOut> marshallableOutSupplier;
    private final boolean metaData;

    BinaryMethodWriterInvocationHandler(final boolean metaData, @NotNull MarshallableOut marshallableOut) {
        this(metaData, () -> marshallableOut);
    }

    public BinaryMethodWriterInvocationHandler(final boolean metaData, Supplier<MarshallableOut> marshallableOutSupplier) {
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
                Jvm.rethrow(t);
            }
        }
    }
}
