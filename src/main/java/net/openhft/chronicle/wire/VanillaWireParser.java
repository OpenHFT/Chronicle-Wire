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

import net.openhft.chronicle.core.util.CharSequenceComparator;
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

    public VanillaWireParser(WireParselet<O> defaultConsumer) {
        this.defaultConsumer = defaultConsumer;
    }

    @Override
    public WireParselet<O> getDefaultConsumer() {
        return defaultConsumer;
    }

    public void parseOne(@NotNull WireIn wireIn, O out) {
        @NotNull ValueIn valueIn = wireIn.readEventName(sb);
        WireParselet<O> consumer = lookup(sb);
        if (consumer == null)
            consumer = getDefaultConsumer();
        consumer.accept(sb, valueIn, out);
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
