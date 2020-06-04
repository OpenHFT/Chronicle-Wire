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

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.MethodReaderBuilder;
import net.openhft.chronicle.bytes.MethodReaderInterceptorReturns;
import org.jetbrains.annotations.NotNull;

public class VanillaMethodReaderBuilder implements MethodReaderBuilder {
    private final MarshallableIn in;
    private boolean warnMissing = false;
    private boolean ignoreDefaults;
    private WireParselet defaultParselet;
    private MethodReaderInterceptorReturns methodReaderInterceptorReturns;

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
                VanillaMethodReader.LOGGER.warn(errorMsg(s, history, sourceIndex));
            else if (VanillaMethodReader.LOGGER.isDebugEnabled())
                VanillaMethodReader.LOGGER.debug(errorMsg(s, history, sourceIndex));
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

    @NotNull
    public MethodReader build(Object... impls) {
        WireParselet defaultParselet = this.defaultParselet;
        if (defaultParselet == null)
            defaultParselet = createDefaultParselet(warnMissing);
        return new VanillaMethodReader(in, ignoreDefaults, defaultParselet, methodReaderInterceptorReturns, impls);
    }
}
