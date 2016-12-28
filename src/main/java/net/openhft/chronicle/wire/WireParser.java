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
        @NotNull ValueIn valueIn = wireIn.readEventName(sb);
        WireParselet<O> consumer = lookup(sb);
        if (consumer == null)
            consumer = getDefaultConsumer();
        consumer.accept(sb, valueIn, out);
    }

    @Override
    default void accept(@NotNull WireIn wireIn, O marshallableOut) {
        while (wireIn.bytes().readRemaining() > 0) {
            parseOne(wireIn, marshallableOut);
            wireIn.consumePadding();
        }
    }

    WireParselet<O> lookup(CharSequence name);

    VanillaWireParser<O> register(WireKey key, WireParselet<O> valueInConsumer);

    WireParselet<O> lookup(int number);
}
