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

/*
 * Created by Peter Lawrey on 25/03/16.
 */
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
    protected void handleInvoke(Method method, Object[] args) {
        MarshallableOut marshallableOut = this.marshallableOutSupplier.get();
        @NotNull DocumentContext context = marshallableOut.writingDocument(metaData);
        try {
            Wire wire = context.wire();
            handleInvoke(method, args, wire);
            wire.padToCacheAlign();
        } catch (Throwable t) {
            context.rollbackOnClose();
            Jvm.rethrow(t);
        } finally {
            context.close();
        }
    }
}
