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

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public interface WireParser {
    WireKey DEFAULT = () -> ":default:";

    @NotNull
    static WireParser wireParser() {
        return new VanillaWireParser();
    }

    default void parse(@NotNull WireIn wireIn) {
        StringBuilder sb = Wires.SBP.acquireStringBuilder();
        ValueIn valueIn = wireIn.read(sb);
        Consumer<ValueIn> consumer = lookup(sb);
        if (consumer == null)
            consumer = lookup(DEFAULT.name());
        if (consumer == null)
            throw new IllegalArgumentException("Unhandled event type " + sb);
        consumer.accept(valueIn);
    }

    Consumer<ValueIn> lookup(CharSequence name);

    default void setDefault(Consumer<ValueIn> valueInConsumer) {
        register(DEFAULT, valueInConsumer);
    }

    void register(WireKey key, Consumer<ValueIn> valueInConsumer);

    Consumer<ValueIn> lookup(int number);
}
