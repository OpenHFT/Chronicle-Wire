/*
 * Copyright 2016-2025 chronicle.software
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
 * A {@link MethodWriterInvocationHandler} that serialises method calls to a
 * text-based {@link MarshallableOut} such as YAML or JSON. Each invocation
 * is wrapped in a {@link WriteDocumentContext} and numeric arguments annotated
 * with {@link LongConversion} are rendered via the associated
 * {@link LongConverter}.
 */
public class TextMethodWriterInvocationHandler extends AbstractMethodWriterInvocationHandler {
    /** Supplier for the target {@link MarshallableOut}. */
    @NotNull
    private final Supplier<MarshallableOut> marshallableOutSupplier;
    /** Caches argument converters keyed by {@link Method}. */
    private final Map<Method, Consumer<Object[]>> visitorConverter = new LinkedHashMap<>();

    /**
     * Creates a handler bound to the supplied interface and target.
     *
     * @param tClass          primary interface for the method writer
     * @param marshallableOut destination for text wire output
     */
    TextMethodWriterInvocationHandler(Class<?> tClass, @NotNull MarshallableOut marshallableOut) {
        this(tClass, () -> marshallableOut);
    }

    /**
     * Creates a handler that obtains its output destination lazily.
     *
     * @param tClass                  primary interface for the method writer
     * @param marshallableOutSupplier supplier of the text-based output
     */
    public TextMethodWriterInvocationHandler(Class<?> tClass, @NotNull Supplier<MarshallableOut> marshallableOutSupplier) {
        super(tClass);
        this.marshallableOutSupplier = marshallableOutSupplier;
    }

    /**
     * Handles a direct {@code writingDocument()} call on the proxy before
     * deferring to the superclass for normal method processing.
     */
    @Override
    protected Object doInvoke(Object proxy, Method method, Object[] args) {
        if (method.getName().equals("writingDocument") && method.getParameterCount() == 0) {
            MarshallableOut marshallableOut = this.marshallableOutSupplier.get();
            return marshallableOut.writingDocument();
        }
        return super.doInvoke(proxy, method, args);
    }

    /**
     * Converts arguments if needed and writes the method call to the underlying
     * text wire within a {@link WriteDocumentContext}.
     */
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

    /** Shared no-op argument converter. */
    static final Consumer<Object[]> NOOP_CONSUMER = Jvm.uncheckedCast(Mocker.ignored(Consumer.class));

    /**
     * Builds or retrieves a converter for the first parameter of {@code method}.
     * If it has a {@link LongConversion} annotation (or one annotated with it),
     * numbers are wrapped in {@link RawText} using the stated
     * {@link LongConverter}.
     *
     * @param method source of parameter annotations
     * @return argument consumer for that method
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
     * Creates a consumer that converts the first argument to
     * {@link RawText} using the provided {@link LongConverter} type.
     * The type may expose a static {@code INSTANCE} or be
     * instantiated reflectively.
     *
     * @param value {@link LongConverter} class or annotation holding one
     * @return argument consumer applying the conversion
     * @throws RuntimeException if reflection fails
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
