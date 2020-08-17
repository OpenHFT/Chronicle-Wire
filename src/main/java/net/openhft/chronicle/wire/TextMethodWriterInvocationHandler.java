/*
 * Copyright 2016-2020 Chronicle Software
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
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TextMethodWriterInvocationHandler extends AbstractMethodWriterInvocationHandler {
    @NotNull
    private final Supplier<MarshallableOut> marshallableOutSupplier;
    private final CountingDocumentContext context = new CountingDocumentContext();
    private final Map<Method, Consumer<Object[]>> visitorConverter = new LinkedHashMap<>();
    private boolean metaData;

    TextMethodWriterInvocationHandler(@NotNull MarshallableOut marshallableOut) {
        this(() -> marshallableOut);
    }

    public TextMethodWriterInvocationHandler(Supplier<MarshallableOut> marshallableOutSupplier) {
        this.marshallableOutSupplier = marshallableOutSupplier;
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
        visitorConverter.computeIfAbsent(method, this::buildConverter)
                .accept(args);

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
//            wire.padToCacheAlign();

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

    private Consumer<Object[]> buildConverter(Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        if (parameterAnnotations.length <= 0)
            return NoOp.INSTANCE;
        for (Annotation anno : parameterAnnotations[0]) {
            if (anno instanceof LongConversion) {
                LongConversion longConversion = (LongConversion) anno;
                LongConverter ic = ObjectUtils.newInstance(longConversion.value());
                return a -> {
                    if (a[0] instanceof Number) {
                        StringBuilder sb = Wires.acquireStringBuilder();
                        ic.append(sb, ((Number) a[0]).longValue());
                        a[0] = new RawText(sb);
                    }
                };
            }
            if (anno instanceof IntConversion) {
                IntConversion intConversion = (IntConversion) anno;
                IntConverter ic = ObjectUtils.newInstance(intConversion.value());
                return a -> {
                    if (a[0] instanceof Number) {
                        StringBuilder sb = Wires.acquireStringBuilder();
                        ic.append(sb, ((Number) a[0]).intValue());
                        a[0] = new RawText(sb);
                    }
                };
            }
        }
        return NoOp.INSTANCE;
    }

    enum NoOp implements Consumer<Object[]> {
        INSTANCE;

        @Override
        public void accept(Object[] objects) {
        }
    }
}
