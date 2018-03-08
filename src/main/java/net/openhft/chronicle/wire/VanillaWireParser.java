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
import net.openhft.chronicle.core.util.CharSequenceComparator;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * A simple parser to associate actions based on events/field names received.
 */
public class VanillaWireParser<O> implements WireParser<O> {
    private final Map<CharSequence, WireParselet<O>> namedConsumer = new TreeMap<>(CharSequenceComparator.INSTANCE);
    private final Map<Integer, WireParselet<O>> numberedConsumer = new HashMap<>();
    private final WireParselet<O> defaultConsumer;
    private final StringBuilder sb = new StringBuilder(128);
    private final StringBuilder lastEventName = new StringBuilder(128);
    private FieldNumberParselet<O> fieldNumberParselet;
    private WireParselet<O> lastParslet = null;
    private long lastStart = 0;

    public VanillaWireParser(WireParselet<O> defaultConsumer,
                             FieldNumberParselet<O> fieldNumberParselet) {
        this.defaultConsumer = defaultConsumer;
        lastEventName.appendCodePoint(0xFFFF);
        this.fieldNumberParselet = fieldNumberParselet;
    }

    private int peekCode(@net.openhft.chronicle.core.annotation.NotNull WireIn wireIn) {
        return wireIn.bytes().peekUnsignedByte();
    }

    @Override
    public WireParselet<O> getDefaultConsumer() {
        return defaultConsumer;
    }

    public void parseOne(@NotNull WireIn wireIn, O out) {

        if (fieldNumberParselet != null && peekCode(wireIn) == BinaryWireCode.FIELD_NUMBER) {
            fieldNumberParselet.readOne(wireIn.readEventNumber(), wireIn, out);
            return;
        }

        long start = wireIn.bytes().readPosition();
        @NotNull ValueIn valueIn = wireIn.readEventName(sb);
        WireParselet<O> parslet;
        // on the assumption most messages are the same as the last,
        // save having to lookup a TreeMap.
        if (StringUtils.isEqual(sb, lastEventName)) {
            parslet = lastParslet;

        } else {
            if (sb.length() > 0 && sb.charAt(0) >= '0' && sb.charAt(0) <= '9') {
                //Must be methodId since Java method-name cannot start with a digit.
                parslet = lookup(parseInt(sb));
            } else {
                parslet = lookup(sb);
            }
            if (parslet == null) {
                if (sb.length() == 0) {
                    // invalid rather than unknown method.
                    Jvm.warn().on(getClass(),
                            "Attempt to read method name/id but not at the start of a method, the previous method name was "
                                    + lastEventName + "\n" + wireIn.bytes().toHexString(start, 1024));
                    if (lastStart < start && lastStart + 1024 >= start)
                        Jvm.warn().on(getClass(),
                                "The previous message was\n" + wireIn.bytes().toHexString(lastStart, start - lastStart));
                }
                parslet = getDefaultConsumer();
            }
        }

        parslet.accept(sb, valueIn, out);
        lastEventName.setLength(0);
        lastEventName.append(sb);
        lastParslet = parslet;
        lastStart = start;
    }

    private static int parseInt(CharSequence sb) {
        int acc = 0;
        for (int i = 0; i < sb.length(); ++i) {
            char ch = sb.charAt(i);
            if (ch >= '0' && ch <= '9') {
                acc *= 10;
                acc += ch - '0';
            } else {
                throw new IllegalStateException(String.format("Cannot parse %s as an int.", sb));
            }
        }
        return acc;
    }

    @NotNull
    @Override
    public VanillaWireParser<O> register(@NotNull WireKey key, WireParselet<O> valueInConsumer) {
        namedConsumer.put(key.name(), valueInConsumer);
        numberedConsumer.put(key.code(), valueInConsumer);
        return this;
    }

    @Override
    public WireParselet<O> lookup(CharSequence name) {
        return namedConsumer.get(name);
    }

    @Override
    public WireParselet<O> lookup(int number) {
        return numberedConsumer.get(number);
    }
}
