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

import java.util.function.Consumer;

public interface WireParser {
    WireKey DEFAULT = () -> ":default:";

    static WireParser wireParser() {
        return new VanillaWireParser();
    }

    default void parse(WireIn wireIn) {
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
