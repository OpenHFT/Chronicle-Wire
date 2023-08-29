/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.util.InvocationTargetRuntimeException;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Defines the interface for parsing arbitrary field-value data from wire input.
 */
public interface WireParser extends Consumer<WireIn> {

    /**
     * A predefined parselet that skips all readable bytes in the wire.
     */
    FieldNumberParselet SKIP_READABLE_BYTES = WireParser::skipReadable;

    /**
     * Creates a new WireParser with a default consumer.
     *
     * @param defaultConsumer The default consumer that handles the wire data when no specific handler is provided.
     * @return A new WireParser instance.
     */
    @NotNull
    static WireParser wireParser(WireParselet defaultConsumer) {
        return new VanillaWireParser(defaultConsumer, SKIP_READABLE_BYTES);
    }

    /**
     * Creates a new WireParser with a default consumer and a custom field number parselet.
     *
     * @param defaultConsumer     The default consumer to handle the wire data when no specific handler is provided.
     * @param fieldNumberParselet Custom field number parselet to handle field numbers in the wire data.
     * @return A new WireParser instance.
     */
    @NotNull
    static WireParser wireParser(@NotNull WireParselet defaultConsumer,
                                 @NotNull FieldNumberParselet fieldNumberParselet) {
        return new VanillaWireParser(defaultConsumer, fieldNumberParselet);
    }

    /**
     * Skips all readable bytes in the wire.
     *
     * @param ignoreMethodId The method ID to ignore. (Currently unused in the method's logic)
     * @param wire           The wire input source.
     */
    static void skipReadable(long ignoreMethodId, WireIn wire) {
        Bytes<?> bytes = wire.bytes();
        bytes.readPosition(bytes.readLimit());
    }

    /**
     * Retrieves the default consumer of this parser.
     *
     * @return The default consumer.
     */
    WireParselet getDefaultConsumer();

    /**
     * Parses a single field-value data from the provided wire input.
     *
     * @param wireIn The wire input source.
     * @throws InvocationTargetRuntimeException When there's a failure invoking the target action for a field.
     * @throws InvalidMarshallableException     When the wire data cannot be marshaled into the desired format.
     */
    void parseOne(@NotNull WireIn wireIn) throws InvocationTargetRuntimeException, InvalidMarshallableException;

    /**
     * Default implementation for the Consumer's accept method. This method
     * reads and processes data from the wire input, invoking the appropriate
     * parselets for handling the data.
     *
     * @param wireIn The wire input source.
     */
    @Override
    default void accept(@NotNull WireIn wireIn) {
        wireIn.startEvent();
        Bytes<?> bytes = wireIn.bytes();
        while (bytes.readRemaining() > 0) {
            if (wireIn.isEndEvent())
                break;
            long start = bytes.readPosition();
            parseOne(wireIn);
            wireIn.consumePadding();
            if (bytes.readPosition() == start) {
                Jvm.warn().on(getClass(), "Failed to progress reading " + bytes.readRemaining() + " bytes left.");
                break;
            }
        }
        wireIn.endEvent();
    }

    /**
     * Searches for a {@link WireParselet} associated with a given name.
     *
     * @param name The name to search the associated {@link WireParselet} for.
     * @return The found {@link WireParselet}, or {@code null} if not found.
     */
    WireParselet lookup(CharSequence name);

    /**
     * Attempts to register a new {@link WireParselet} for a given key. If a parselet
     * is already registered with the same key, a warning is emitted and the new
     * registration is ignored.
     *
     * @param key            The key to associate the parselet with.
     * @param valueInConsumer The parselet to register.
     * @return This instance for method chaining.
     */
    @NotNull
    default VanillaWireParser registerOnce(WireKey key, WireParselet valueInConsumer) {
        CharSequence name = key.name();
        if (lookup(name) != null) {
            Jvm.warn().on(getClass(), "Unable to register multiple methods for " + name + " ignoring one.");
        } else {
            register(key, valueInConsumer);
        }
        return (VanillaWireParser) this;
    }

    /**
     * Registers a new {@link WireParselet} for a given key.
     *
     * @param key            The key to associate the parselet with.
     * @param valueInConsumer The parselet to register.
     * @return This instance for method chaining.
     */
    @NotNull
    default VanillaWireParser register(WireKey key, WireParselet valueInConsumer) {
        return register(key.toString(), valueInConsumer);
    }

    /**
     * Registers a new {@link WireParselet} for a given key name.
     *
     * @param keyName        The name of the key to associate the parselet with.
     * @param valueInConsumer The parselet to register.
     * @return This instance for method chaining.
     */
    @NotNull
    VanillaWireParser register(String keyName, WireParselet valueInConsumer);

}
