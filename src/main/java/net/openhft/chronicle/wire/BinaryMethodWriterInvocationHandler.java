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

import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.function.Supplier;


public class BinaryMethodWriterInvocationHandler extends AbstractMethodWriterInvocationHandler {
    @NotNull
    private final Supplier<MarshallableOut> marshallableOutSupplier;
    private final boolean metaData;
    @NotNull
    private final CountingDocumentContext context = new CountingDocumentContext();

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
            context.count = 0;
            return context.dc(marshallableOut.writingDocument(metaData));
        }
        return super.doInvoke(proxy, method, args);
    }

    @Override
    protected void handleInvoke(Method method, Object[] args) {
        DocumentContext dc = context.dc();
        boolean chained = method.getReturnType().isInterface();
        if (dc == null) {
            MarshallableOut marshallableOut = this.marshallableOutSupplier.get();
            dc = marshallableOut.writingDocument(metaData);
            if (chained)
                context.dc(dc);
            context.local = true;
        }
        try {
            Wire wire = dc.wire();
            handleInvoke(method, args, wire);
            wire.padToCacheAlign();

        } catch (Throwable t) {
            dc.rollbackOnClose();
            Jvm.rethrow(t);

        } finally {
            if (!chained) {
                if (context.local) {
                    dc.close();
                    context.dc(null);
                    context.local = false;
                } else {
                    context.count++;
                }
            }
        }
    }
}
