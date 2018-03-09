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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

/**
 * Interface to parseOne arbitrary field-value data.
 */
public interface WireParser<O> extends BiConsumer<WireIn, O> {

    FieldNumberParselet NO_OP = WireParser::noOpReadOne;

    @NotNull
    static <O> WireParser<O> wireParser(WireParselet<O> defaultConsumer) {
        return new VanillaWireParser<>(defaultConsumer, NO_OP);
    }

    @NotNull
    static <O> WireParser<O> wireParser(@NotNull WireParselet<O> defaultConsumer,
                                        @NotNull FieldNumberParselet<O> fieldNumberParselet) {
        return new VanillaWireParser<>(defaultConsumer, fieldNumberParselet);
    }

    static <T> void noOpReadOne(long ignoreMethodId, WireIn wire, T o) {
        wire.bytes().writePosition(wire.bytes().readLimit());
    }

    WireParselet<O> getDefaultConsumer();

    void parseOne(@NotNull WireIn wireIn, O out);

    @Override
    default void accept(@NotNull WireIn wireIn, O marshallableOut) {
        Bytes<?> bytes = wireIn.bytes();
        while (bytes.readRemaining() > 0) {
            long start = bytes.readPosition();
            parseOne(wireIn, marshallableOut);
            wireIn.consumePadding();
            if (bytes.readPosition() == start) {
                Jvm.warn().on(getClass(), "Failed to progress reading " + bytes.readRemaining() + " bytes left.");
                break;
            }
        }
    }

    WireParselet<O> lookup(CharSequence name);

    @NotNull
    default VanillaWireParser<O> registerOnce(WireKey key, WireParselet<O> valueInConsumer) {
        CharSequence name = key.name();
        if (lookup(name) != null) {
            Jvm.warn().on(getClass(), "Unable to register multiple methods for " + name + " ignoring one.");
        } else {
            register(key, valueInConsumer);
        }
        return (VanillaWireParser<O>) this;
    }

    @NotNull
    VanillaWireParser<O> register(WireKey key, WireParselet<O> valueInConsumer);

    WireParselet<O> lookup(int number);
}
