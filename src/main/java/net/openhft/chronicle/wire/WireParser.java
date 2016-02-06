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

import java.util.function.BiConsumer;

/**
 * Interface to parseOne arbitrary field-value data.
 */
public interface WireParser<O> extends BiConsumer<WireIn, O> {
    @NotNull
    static <O> WireParser<O> wireParser(WireParselet<O> defaultConsumer) {
        return new VanillaWireParser<>(defaultConsumer);
    }

    WireParselet<O> getDefaultConsumer();

    default void parseOne(@NotNull WireIn wireIn, O out) {
        StringBuilder sb = WireInternal.SBP.acquireStringBuilder();
        ValueIn valueIn = wireIn.readEventName(sb);
        WireParselet<O> consumer = lookup(sb);
        if (consumer == null)
            consumer = getDefaultConsumer();
        consumer.accept(sb, valueIn, out);
    }

    @Override
    default void accept(WireIn wireIn, O marshallableOut) {
        while (wireIn.bytes().readRemaining() > 0) {
            parseOne(wireIn, marshallableOut);
            consume(wireIn, ',');
            consume(wireIn, '}');
            wireIn.consumePadding();
        }
    }

    default void consume(WireIn wireIn, char ch) {
        if (wireIn.bytes().peekUnsignedByte() == ch) {
            wireIn.bytes().readSkip(1);
        }
    }

    WireParselet<O> lookup(CharSequence name);

    VanillaWireParser<O> register(WireKey key, WireParselet<O> valueInConsumer);

    WireParselet<O> lookup(int number);
}
