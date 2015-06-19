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
