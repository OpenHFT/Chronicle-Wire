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

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.MethodReaderBuilder;
import net.openhft.chronicle.bytes.MethodReaderInterceptorReturns;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class VanillaMethodReaderBuilder implements MethodReaderBuilder {
    private static final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();
    private static final Class<?> COMPILE_FAILED = ClassNotFoundException.class;

    private final MarshallableIn in;
    private boolean warnMissing = false;
    private boolean ignoreDefaults;
    private WireParselet defaultParselet;
    private MethodReaderInterceptorReturns methodReaderInterceptorReturns;
    private WireType wireType;
    private Object[] metaDataHandler = null;

    public VanillaMethodReaderBuilder(MarshallableIn in) {
        this.in = in;
    }

    // TODO add support for filtering.

    @NotNull
    public static WireParselet createDefaultParselet(boolean warnMissing) {
        return (s, v) -> {
            MessageHistory history = MessageHistory.get();
            long sourceIndex = history.lastSourceIndex();
            v.skipValue();
            if (s.length() == 0 || warnMissing)
                Jvm.warn().on(VanillaMethodReader.class, errorMsg(s, history, sourceIndex));
            else if (Jvm.isDebugEnabled(VanillaMethodReader.class))
                Jvm.debug().on(VanillaMethodReader.class, errorMsg(s, history, sourceIndex));
        };
    }

    @NotNull
    private static String errorMsg(CharSequence s, MessageHistory history, long sourceIndex) {

        final String identifierType = s.length() != 0 && Character.isDigit(s.charAt(0)) ? "@MethodId" : "method-name";
        return "Unknown " + identifierType + "='" + s + "' from " + history.lastSourceId() + " at " +
                Long.toHexString(sourceIndex) + " ~ " + (int) sourceIndex;
    }

    public boolean ignoreDefaults() {
        return ignoreDefaults;
    }

    @NotNull
    public MethodReaderBuilder ignoreDefaults(boolean ignoreDefaults) {
        this.ignoreDefaults = ignoreDefaults;
        return this;
    }

    public WireParselet defaultParselet() {
        return defaultParselet;
    }

    public MethodReaderBuilder defaultParselet(WireParselet defaultParselet) {
        this.defaultParselet = defaultParselet;
        return this;
    }

    public VanillaMethodReaderBuilder methodReaderInterceptorReturns(MethodReaderInterceptorReturns methodReaderInterceptorReturns) {
        this.methodReaderInterceptorReturns = methodReaderInterceptorReturns;
        return this;
    }

    public boolean warnMissing() {
        return warnMissing;
    }

    public VanillaMethodReaderBuilder warnMissing(boolean warnMissing) {
        this.warnMissing = warnMissing;
        return this;
    }

    public WireType wireType() {
        return wireType;
    }

    public VanillaMethodReaderBuilder wireType(WireType wireType) {
        this.wireType = wireType;
        return this;
    }

    @Nullable
    private MethodReader createGeneratedInstance(Supplier<MethodReader> vanillaSupplier, Object... impls) {
        // todo: support this options in the generated code
        if (ignoreDefaults)
            return null;

        GenerateMethodReader generateMethodReader = new GenerateMethodReader(wireType, methodReaderInterceptorReturns, metaDataHandler, impls);

        String fullClassName = generateMethodReader.packageName() + "." + generateMethodReader.generatedClassName();

        try {
            try {
                final Class<?> generatedClass = Class.forName(fullClassName);

                return instanceForGeneratedClass(vanillaSupplier, generatedClass, impls);
            } catch (ClassNotFoundException e) {
                Class<?> clazz = classCache.computeIfAbsent(fullClassName, name -> generateMethodReader.createClass());
                if (clazz != null && clazz != COMPILE_FAILED) {
                    return instanceForGeneratedClass(vanillaSupplier, clazz, impls);
                }
            }
        } catch (Throwable e) {
            classCache.put(fullClassName, COMPILE_FAILED);
            // do nothing and drop through
            Jvm.warn().on(getClass(), "Failed to compile generated method reader - falling back to proxy method reader. Please report this failure.", e);
        }

        return null;
    }

    @NotNull
    private MethodReader instanceForGeneratedClass(Supplier<MethodReader> vanillaSupplier,
                                                   Class<?> generatedClass, Object[] impls
    ) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        final Constructor<?> constructor = generatedClass.getConstructors()[0];

        WireParselet debugLoggingParselet = VanillaMethodReader::logMessage;

        return (MethodReader) constructor.newInstance(
                in, debugLoggingParselet, vanillaSupplier, methodReaderInterceptorReturns, metaDataHandler, impls);
    }

    @Override
    public MethodReaderBuilder metaDataHandler(Object... components) {
        this.metaDataHandler = components;
        return this;
    }

    @NotNull
    public MethodReader build(Object... impls) {
        final WireParselet defaultParselet = this.defaultParselet == null ?
                createDefaultParselet(warnMissing) : this.defaultParselet;

        Supplier<MethodReader> vanillaSupplier = () -> new VanillaMethodReader(
                in, ignoreDefaults, defaultParselet, methodReaderInterceptorReturns, metaDataHandler, impls);

        final MethodReader generatedInstance = createGeneratedInstance(vanillaSupplier, impls);

        return generatedInstance == null ? vanillaSupplier.get() : generatedInstance;
    }
}
