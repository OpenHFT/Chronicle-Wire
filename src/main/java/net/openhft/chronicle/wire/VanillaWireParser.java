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
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.util.CharSequenceComparator;
import net.openhft.chronicle.core.util.InvocationTargetRuntimeException;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * A simple parser to associate actions based on events/field names received.
 */
public class VanillaWireParser implements WireParser {
    private final Map<CharSequence, WireParselet> namedConsumer = new TreeMap<>(CharSequenceComparator.INSTANCE);
    private final Map<Integer, Map.Entry<String, WireParselet>> numberedConsumer = new HashMap<>();
    private final WireParselet defaultConsumer;
    private final StringBuilder sb = new StringBuilder(128);
    private final StringBuilder lastEventName = new StringBuilder(128);
    private FieldNumberParselet fieldNumberParselet;
    private WireParselet lastParslet = null;
    private long lastStart = 0;

    public VanillaWireParser(@NotNull WireParselet defaultConsumer,
                             @NotNull FieldNumberParselet fieldNumberParselet) {
        this.defaultConsumer = defaultConsumer;
        lastEventName.appendCodePoint(0xFFFF);
        this.fieldNumberParselet = fieldNumberParselet;
    }

    private int peekCode(@NotNull WireIn wireIn) {
        return wireIn.bytes().peekUnsignedByte();
    }

    @Override
    public WireParselet getDefaultConsumer() {
        return defaultConsumer;
    }

    public void parseOne(@NotNull WireIn wireIn) throws InvocationTargetRuntimeException, InvalidMarshallableException {
        long start = wireIn.bytes().readPosition();
        if (peekCode(wireIn) == BinaryWireCode.FIELD_NUMBER) {
            parseOneBinary(wireIn);
            return;
        }

        @NotNull ValueIn valueIn = wireIn.readEventName(sb);
        WireParselet parslet;
        // on the assumption most messages are the same as the last,
        // save having to lookup a TreeMap.
        if (StringUtils.isEqual(sb, lastEventName)) {
            parslet = lastParslet;

        } else {
            parslet = lookup(sb);
            if (parslet == null) {
                if (sb.length() == 0) {
                    parseOneEmpty(wireIn, start);
                }
                parslet = getDefaultConsumer();
            }
        }

        parslet.accept(sb, valueIn);
        lastEventName.setLength(0);
        lastEventName.append(sb);
        lastParslet = parslet;
        lastStart = start;
    }

    private void parseOneEmpty(@NotNull WireIn wireIn, long start) {
        // invalid rather than unknown method.
        Jvm.warn().on(getClass(),
                "Attempt to read method name/id but not at the start of a method, the previous method name was "
                        + lastEventName + "\n" + wireIn.bytes().toHexString(start, 1024));
        if (lastStart < start && lastStart + 1024 >= start)
            Jvm.warn().on(getClass(),
                    "The previous message was\n" + wireIn.bytes().toHexString(lastStart, start - lastStart));
    }

    private void parseOneBinary(@NotNull WireIn wireIn) throws InvalidMarshallableException {
        long methodId = wireIn.readEventNumber();
        if (methodId == (int) methodId) {
            Map.Entry<String, WireParselet> entry = numberedConsumer.get((int) methodId);
            if (entry != null) {
                WireParselet parselet = entry.getValue();
                parselet.accept(entry.getKey(), wireIn.getValueIn());
                return;
            }
        }
        fieldNumberParselet.readOne(methodId, wireIn);
    }

    @NotNull
    @Override
    public VanillaWireParser register(@NotNull WireKey key, WireParselet valueInConsumer) {
        return register(key.name().toString(), key.code(), valueInConsumer);
    }

    @NotNull
    public VanillaWireParser register(String keyName, WireParselet valueInConsumer) {
        return register(keyName, keyName.hashCode(), valueInConsumer);
    }

    private VanillaWireParser register(String keyName, int code, WireParselet valueInConsumer) {
        namedConsumer.put(keyName, valueInConsumer);
        numberedConsumer.put(code, new AbstractMap.SimpleEntry<>(keyName, valueInConsumer));
        return this;
    }

    @Override
    public WireParselet lookup(CharSequence name) {
        return namedConsumer.get(name);
    }
}
