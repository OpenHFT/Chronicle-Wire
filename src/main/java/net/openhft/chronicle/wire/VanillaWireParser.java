/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.util.CharSequenceComparator;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public class VanillaWireParser implements WireParser {
    final Map<CharSequence, Consumer<ValueIn>> namedConsumer = new TreeMap<>(CharSequenceComparator.INSTANCE);
    final Map<Integer, Consumer<ValueIn>> numberedConsumer = new HashMap<>();

    @Override
    public void register(@NotNull WireKey key, Consumer<ValueIn> valueInConsumer) {
        namedConsumer.put(key.name(), valueInConsumer);
        numberedConsumer.put(key.code(), valueInConsumer);
    }

    @Override
    public Consumer<ValueIn> lookup(CharSequence name) {
        return namedConsumer.get(name);
    }

    @Override
    public Consumer<ValueIn> lookup(int number) {
        return numberedConsumer.get(number);
    }
}
