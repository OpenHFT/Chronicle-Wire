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

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.MethodReaderBuilder;
import net.openhft.chronicle.bytes.MethodReaderInterceptorReturns;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.onoes.ExceptionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static net.openhft.chronicle.wire.WireParser.SKIP_READABLE_BYTES;

public class VanillaMethodReaderBuilder implements MethodReaderBuilder {
    public static final String DISABLE_READER_PROXY_CODEGEN = "disableReaderProxyCodegen";
    private static final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();
    private static final Class<?> COMPILE_FAILED = ClassNotFoundException.class;

    private final MarshallableIn in;
    private boolean ignoreDefaults;
    private WireParselet defaultParselet;
    private MethodReaderInterceptorReturns methodReaderInterceptorReturns;
    private WireType wireType;
    private Object[] metaDataHandler = null;
    private ExceptionHandler exceptionHandlerOnUnknownMethod = Jvm.debug();
    private Predicate predicate = x -> true;

    private boolean scanning = false;

    public VanillaMethodReaderBuilder(MarshallableIn in) {
        this.in = in;
    }

    @NotNull
    public static WireParselet createDefaultParselet(ExceptionHandler exceptionHandlerOnUnknownMethod) {
        return (s, v) -> {
            MessageHistory history = MessageHistory.get();
            long sourceIndex = history.lastSourceIndex();
            v.skipValue();
            ExceptionHandler eh = s.length() == 0
                    ? Jvm.warn()
                    : exceptionHandlerOnUnknownMethod;
            eh.on(VanillaMethodReader.class, errorMsg(s, history, sourceIndex));
        };
    }

    @NotNull
    private static String errorMsg(CharSequence s, MessageHistory history, long sourceIndex) {

        final String identifierType = s.length() != 0 && Character.isDigit(s.charAt(0)) ? "@MethodId" : "method-name";
        String msg = "Unknown " + identifierType + "='" + s + "'";
        if (history.lastSourceId() >= 0)
            msg += " from " + history.lastSourceId() + " at " +
                    Long.toHexString(sourceIndex) + " ~ " + (int) sourceIndex;
        return msg;
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

    @Override
    public MethodReaderBuilder exceptionHandlerOnUnknownMethod(ExceptionHandler exceptionHandler) {
        this.exceptionHandlerOnUnknownMethod = exceptionHandler;
        return this;
    }

    public WireType wireType() {
        return wireType;
    }

    public VanillaMethodReaderBuilder wireType(WireType wireType) {
        this.wireType = wireType;
        return this;
    }

    /**
     * When enabled, readOne() will skip over meta data and unknown events to find at least one event.
     *
     * @param scanning whether to read events until it finds a known one.
     * @return this
     */
    public VanillaMethodReaderBuilder scanning(boolean scanning) {
        this.scanning = scanning;
        return this;
    }

    @Nullable
    private MethodReader createGeneratedInstance(Object... impls) {
        if (ignoreDefaults || Jvm.getBoolean(DISABLE_READER_PROXY_CODEGEN))
            return null;

        GenerateMethodReader generateMethodReader = new GenerateMethodReader(wireType, methodReaderInterceptorReturns, metaDataHandler, impls);

        String fullClassName = generateMethodReader.packageName() + "." + generateMethodReader.generatedClassName();

        try {
            try {
                final Class<?> generatedClass = Class.forName(fullClassName);

                return instanceForGeneratedClass(generatedClass, impls);
            } catch (ClassNotFoundException e) {
                Class<?> clazz = classCache.computeIfAbsent(fullClassName, name -> generateMethodReader.createClass());
                if (clazz != null && clazz != COMPILE_FAILED) {
                    return instanceForGeneratedClass(clazz, impls);
                }
            }
        } catch (Throwable e) {
            classCache.put(fullClassName, COMPILE_FAILED);
            Jvm.warn().on(getClass(), "Failed to compile generated method reader - " +
                    "falling back to proxy method reader. Please report this failure as support for " +
                    "proxy method readers will be dropped in x.25.", e);
        }

        return null;
    }

    @NotNull
    private MethodReader instanceForGeneratedClass(Class<?> generatedClass, Object[] impls)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        final Constructor<?> constructor = generatedClass.getConstructors()[0];

        WireParselet debugLoggingParselet = VanillaMethodReader::logMessage;

        MethodReader reader = (MethodReader) constructor.newInstance(
                in, defaultParselet, debugLoggingParselet, methodReaderInterceptorReturns, metaDataHandler,
                impls);
        if (reader instanceof AbstractGeneratedMethodReader) {
            AbstractGeneratedMethodReader reader0 = (AbstractGeneratedMethodReader) reader;
            reader0.scanning(scanning);
            reader0.predicate(predicate);
        }

        return reader;
    }

    @Override
    public MethodReaderBuilder metaDataHandler(Object... components) {
        this.metaDataHandler = components;
        return this;
    }

    @NotNull
    public MethodReader build(Object... impls) {
        if (this.defaultParselet == null)
            this.defaultParselet = createDefaultParselet(exceptionHandlerOnUnknownMethod);

        final MethodReader generatedInstance = createGeneratedInstance(impls);

        return generatedInstance == null ? new VanillaMethodReader(in, ignoreDefaults, defaultParselet, SKIP_READABLE_BYTES,
                methodReaderInterceptorReturns, metaDataHandler, predicate,
                impls) :
                generatedInstance;
    }

    @Override
    public MethodReaderBuilder predicate(Predicate<?> predicate) {
        this.predicate = predicate;
        return this;
    }
}
