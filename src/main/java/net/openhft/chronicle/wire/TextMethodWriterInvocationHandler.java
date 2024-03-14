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
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.core.util.Mocker;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * This class represents an invocation handler specifically for text method writers. It extends the
 * AbstractMethodWriterInvocationHandler to implement method call behavior for text-based method writers.
 * It mainly converts method calls to textual data using the provided MarshallableOut.
 */
public class TextMethodWriterInvocationHandler extends AbstractMethodWriterInvocationHandler {
    @NotNull
    private final Supplier<MarshallableOut> marshallableOutSupplier;
    private final Map<Method, Consumer<Object[]>> visitorConverter = new LinkedHashMap<>();

    /**
     * Constructor initializing the handler with a MarshallableOut instance.
     *
     * @param tClass           The class for which this invocation handler is being used.
     * @param marshallableOut  The MarshallableOut instance used for data serialization.
     */
    TextMethodWriterInvocationHandler(Class tClass, @NotNull MarshallableOut marshallableOut) {
        this(tClass, () -> marshallableOut);
    }

    /**
     * Constructor initializing the handler with a supplier for MarshallableOut.
     *
     * @param tClass                   The class for which this invocation handler is being used.
     * @param marshallableOutSupplier  The supplier providing instances of MarshallableOut for data serialization.
     */
    public TextMethodWriterInvocationHandler(Class tClass, @NotNull Supplier<MarshallableOut> marshallableOutSupplier) {
        super(tClass);
        this.marshallableOutSupplier = marshallableOutSupplier;
    }

    @Override
    protected Object doInvoke(Object proxy, Method method, Object[] args) {
        if (method.getName().equals("writingDocument") && method.getParameterCount() == 0) {
            MarshallableOut marshallableOut = this.marshallableOutSupplier.get();
            return marshallableOut.writingDocument();
        }
        return super.doInvoke(proxy, method, args);
    }

    @Override
    protected void handleInvoke(Method method, Object[] args) {
        visitorConverter.computeIfAbsent(method, this::buildConverter)
                .accept(args);

        boolean chained = method.getReturnType().isInterface();
        MarshallableOut marshallableOut = this.marshallableOutSupplier.get();
        try (WriteDocumentContext dc = (WriteDocumentContext) marshallableOut.acquireWritingDocument(false)) {
            try {
                dc.chainedElement(chained);
                Wire wire = dc.wire();
                handleInvoke(method, args, wire);
            } catch (Throwable t) {
                dc.rollbackOnClose();
                throw Jvm.rethrow(t);
            }
        }
    }

    static final Consumer<Object[]> NOOP_CONSUMER = Mocker.ignored(Consumer.class);

    /**
     * Builds a converter for method parameters based on the annotations present on the method.
     * It supports long and int conversions based on annotations like @LongConversion and @IntConversion.
     *
     * @param method  The method for which the converter is being built.
     * @return A Consumer that takes in an Object array and performs conversions on it.
     */
    private Consumer<Object[]> buildConverter(Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        // If there are no annotations, return a no-operation consumer.
        if (parameterAnnotations.length == 0)
            return NOOP_CONSUMER;
        for (Annotation anno : parameterAnnotations[0]) {
            if (anno instanceof LongConversion) {
                LongConversion longConversion = (LongConversion) anno;
                final Class<?> value = longConversion.value();

                return buildLongConverter(value);
            }
            LongConversion lc2 = anno.annotationType().getAnnotation(LongConversion.class);
            if (lc2 != null) {
                return buildLongConverter(anno.annotationType());
            }
        }
        return NOOP_CONSUMER;
    }

    /**
     * Creates a long converter based on the provided class. The converter will convert the input
     * object to its long representation using the provided LongConverter. It primarily focuses on
     * handling the conversion of numbers to their textual representation in the format dictated by
     * the provided LongConverter.
     *
     * @param value The class representing the desired LongConverter. The class should ideally have
     *              a public static field named "INSTANCE" which holds a pre-created instance of
     *              the converter. If not, a new instance is created using reflection.
     * @return A Consumer that takes in an Object array and performs the long conversion on its first element.
     * @throws RuntimeException If there is an IllegalAccessException when accessing the "INSTANCE" field.
     */
    @NotNull
    private Consumer<Object[]> buildLongConverter(Class<?> value) {
        LongConverter lc;
        try {
            // Attempt to retrieve a pre-created INSTANCE of the converter.
            lc = (LongConverter) value.getField("INSTANCE").get(null);
        } catch (NoSuchFieldException e) {
            // If there's no INSTANCE field, create a new instance of the converter.
            lc = (LongConverter) ObjectUtils.newInstance(value);
        } catch (IllegalAccessException e) {
            // Throw an exception if there's a problem accessing the field.
            throw new RuntimeException(e);
        }
        LongConverter finalLc = lc;
        return a -> {
            if (a[0] instanceof Number) {
                try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                    StringBuilder sb = stlSb.get();
                    finalLc.append(sb, ((Number) a[0]).longValue());
                    a[0] = new RawText(sb);
                }
            }
        };
    }
}
