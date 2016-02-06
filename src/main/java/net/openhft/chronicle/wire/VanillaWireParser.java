/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

    public VanillaWireParser(WireParselet<O> defaultConsumer) {
        this.defaultConsumer = defaultConsumer;
    }

    @Override
    public WireParselet getDefaultConsumer() {
        return defaultConsumer;
    }

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
