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
 * Defines a contract for parsing field-value data from a {@link WireIn} stream.
 * A {@code WireParser} typically maintains a set of {@link WireParselet} or
 * {@link FieldNumberParselet} instances, each responsible for a particular
 * field name or number encountered in the input. It orchestrates reading the
 * field identifiers and dispatches to the relevant parselet to deserialise the
 * value. As a {@link java.util.function.Consumer}{@code <WireIn>} it can
 * process a whole document or a sequence of events within a wire.
 */
public interface WireParser extends Consumer<WireIn> {

    /**
     * A predefined {@link FieldNumberParselet} that, when invoked, consumes and
     * discards all remaining readable bytes in the current value or document
     * context of the {@link WireIn}. This is often used as a default handler for
     * unknown or uninteresting field numbers.
     */
    FieldNumberParselet SKIP_READABLE_BYTES = WireParser::skipReadable;

    /**
     * Creates a new {@code WireParser} with a default consumer.
     *
     * @param defaultConsumer the {@link WireParselet} to invoke when a field name
     *                        read from the wire does not match any registered
     *                        named parselets. This typically handles unknown
     *                        fields or logs warnings.
     * @return a new {@link VanillaWireParser} configured with the given default
     *         consumer and {@link #SKIP_READABLE_BYTES} for unknown field
     *         numbers.
     */
    @NotNull
    static WireParser wireParser(WireParselet defaultConsumer) {
        return new VanillaWireParser(defaultConsumer, SKIP_READABLE_BYTES);
    }

    /**
     * Creates a new {@code WireParser} with a default consumer and a custom
     * field number parselet.
     *
     * @param defaultConsumer     the {@link WireParselet} for unhandled named
     *                             fields.
     * @param fieldNumberParselet the {@link FieldNumberParselet} to invoke when
     *                             a field number read from a binary wire does not
     *                             match any registered numbered parselets.
     * @return a new {@link VanillaWireParser} instance.
     */
    @NotNull
    static WireParser wireParser(@NotNull WireParselet defaultConsumer,
                                 @NotNull FieldNumberParselet fieldNumberParselet) {
        return new VanillaWireParser(defaultConsumer, fieldNumberParselet);
    }

    /**
     * Skips all readable bytes in the provided wire.
     *
     * @param ignoreMethodId the method id or field number that triggered this
     *                       skip action. It is often unused by the skip logic
     *                       itself but provided for context.
     * @param wire           the {@link WireIn} whose current value's readable
     *                       bytes should be skipped. This advances the read
     *                       position to the end of the current value or context.
     */
    static void skipReadable(long ignoreMethodId, WireIn wire) {
        Bytes<?> bytes = wire.bytes();
        bytes.readPosition(bytes.readLimit());
    }

    /**
     * Retrieves the {@link WireParselet} used when a field name encountered in
     * the input does not match any explicitly registered parselet.
     *
     * @return the default consumer for unmatched field names.
     */
    WireParselet getDefaultConsumer();

    /**
     * Parses a single field-value pair from the given {@link WireIn}. The method
     * reads the field identifier (name or number) and dispatches to the
     * appropriate registered {@link WireParselet} or
     * {@link FieldNumberParselet} to deserialise the value. If no matching
     * parselet is found the {@link #getDefaultConsumer()} or its numbered
     * counterpart is used.
     *
     * @param wireIn The wire input source.
     * @throws InvocationTargetRuntimeException When there's a failure invoking the target action for a field.
     * @throws InvalidMarshallableException     When the wire data cannot be marshaled into the desired format.
     */
    void parseOne(@NotNull WireIn wireIn) throws InvocationTargetRuntimeException, InvalidMarshallableException;

    /**
     * Processes an entire document or event from {@code wireIn}. The default
     * implementation repeatedly calls {@link #parseOne(WireIn)} until all fields
     * within the current event or document have been consumed. Event boundaries
     * are handled using {@link WireIn#startEvent()} and
     * {@link WireIn#endEvent()}.
     *
     * @param wireIn the wire input source
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
     * @param key            the {@link WireKey} (providing both name and code)
     *                       to associate with the parselet
     * @param valueInConsumer the {@link WireParselet} to register for the key
     * @return this {@code WireParser} instance, cast to {@link VanillaWireParser},
     *         for fluent configuration
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
     * Registers a {@link WireParselet} to handle fields matching the supplied
     * {@link WireKey}. This typically registers the parselet for both the key's
     * name and its code. If a parselet is already registered for the name this
     * may overwrite it or log a warning, depending on the implementation (see
     * {@link #registerOnce}).
     *
     * @param key            the key to associate with the parselet
     * @param valueInConsumer the parselet to register
     * @return this instance for method chaining
     */
    @NotNull
    default VanillaWireParser register(WireKey key, WireParselet valueInConsumer) {
        return register(key.toString(), valueInConsumer);
    }

    /**
     * Registers a {@link WireParselet} to handle fields matching the provided
     * {@code keyName}. This typically also registers the parselet for a code
     * derived from {@code keyName.hashCode()}.
     *
     * @param keyName        the name of the key to associate with the parselet
     * @param valueInConsumer the parselet to register
     * @return this instance for method chaining
     */
    @NotNull
    VanillaWireParser register(String keyName, WireParselet valueInConsumer);

}
