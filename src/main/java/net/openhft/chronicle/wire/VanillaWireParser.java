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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.util.CharSequenceComparator;
import net.openhft.chronicle.core.util.InvocationTargetRuntimeException;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Provides an implementation of {@link WireParser} that maps field names or ids to
 * {@link WireParselet}s. Named parselets are looked up via {@link #namedConsumer} and
 * numbered fields via {@link #numberedConsumer}. A {@link #defaultConsumer} handles
 * fields that are not explicitly registered and {@link #fieldNumberParselet} deals with
 * unmapped numeric identifiers.
 */
public class VanillaWireParser implements WireParser {

    /**
     * {@link TreeMap} of field names to parselets. {@link CharSequenceComparator#INSTANCE}
     * provides stable ordering and lookup semantics.
     */
    private final Map<CharSequence, WireParselet> namedConsumer = new TreeMap<>(CharSequenceComparator.INSTANCE);

    /**
     * {@link HashMap} of numeric field ids to the original name and parselet.
     */
    private final Map<Integer, Map.Entry<String, WireParselet>> numberedConsumer = new HashMap<>();

    /**
     * Invoked when a field name is not present in {@link #namedConsumer}.
     */
    private final WireParselet defaultConsumer;

    /**
     * Reusable buffer for reading textual field names.
     */
    private final StringBuilder sb = new StringBuilder(128);

    /**
     * Caches the most recently parsed field name.
     */
    private final StringBuilder lastEventName = new StringBuilder(128);

    /**
     * Called when a numeric field id is not mapped in {@link #numberedConsumer}.
     */
    private FieldNumberParselet fieldNumberParselet;

    /**
     * Cache of the parselet associated with {@link #lastEventName}.
     */
    private WireParselet lastParslet = null;

    /**
     * Start position of the last parsed event for debugging purposes.
     */
    private long lastStart = 0;

    /**
     * Constructs a new VanillaWireParser with the specified default consumer and field number parselet.
     *
     * @param defaultConsumer      consumer for unregistered named fields. Must not be {@code null}.
     * @param fieldNumberParselet  handler for unregistered numeric ids. Must not be {@code null}.
     */
    public VanillaWireParser(@NotNull WireParselet defaultConsumer,
                             @NotNull FieldNumberParselet fieldNumberParselet) {
        this.defaultConsumer = defaultConsumer;

        // Initializing the lastEventName with a non-ASCII value to ensure uniqueness.
        lastEventName.appendCodePoint(0xFFFF);
        this.fieldNumberParselet = fieldNumberParselet;
    }

    /**
     * Returns the next byte in {@code wireIn} without altering its read position.
     * The value is returned as an unsigned int.
     */
    private int peekCode(@NotNull WireIn wireIn) {
        return wireIn.bytes().peekUnsignedByte();
    }

    @Override
    public WireParselet getDefaultConsumer() {
        return defaultConsumer;
    }

    /**
     * Reads one field from {@code wireIn}. Binary fields (identified by
     * {@link BinaryWireCode#FIELD_NUMBER}) are handled by
     * {@link #parseOneBinary(WireIn)}. Otherwise the field name is read into
     * {@link #sb} and the matching parselet located via {@link #namedConsumer}.
     * If no match is found, {@link #getDefaultConsumer()} is used. The chosen
     * parselet and name are cached for the next call.
     *
     * @param wireIn the source of the field
     * @throws InvocationTargetRuntimeException if a parselet throws a checked exception
     * @throws InvalidMarshallableException     if the wire data is malformed
     */
    public void parseOne(@NotNull WireIn wireIn) throws InvocationTargetRuntimeException, InvalidMarshallableException {
        long start = wireIn.bytes().readPosition();

        // Check if it's binary data by peeking the code.
        if (peekCode(wireIn) == BinaryWireCode.FIELD_NUMBER) {
            parseOneBinary(wireIn);
            return;
        }

        @NotNull ValueIn valueIn = wireIn.readEventName(sb);
        WireParselet parslet;

        // Check if the event name is the same as the previous one to avoid unnecessary TreeMap lookup.
        if (StringUtils.isEqual(sb, lastEventName)) {
            parslet = lastParslet;

        } else {
            parslet = lookup(sb);

            // If the parselet wasn't found and the event name is empty, handle the empty event name.
            if (parslet == null) {
                if (sb.length() == 0) {
                    parseOneEmpty(wireIn, start);
                }
                parslet = getDefaultConsumer();
            }
        }

        parslet.accept(sb, valueIn);

        // Update the last event name, last parslet, and last start position for the next parse.
        lastEventName.setLength(0);
        lastEventName.append(sb);
        lastParslet = parslet;
        lastStart = start;
    }

    /**
     * Called when {@link ValueIn#text()} returns an empty name. Emits a warning
     * with the surrounding bytes to aid debugging.
     *
     * @param wireIn source of the bytes
     * @param start  position at which the field began
     */
    private void parseOneEmpty(@NotNull WireIn wireIn, long start) {
        // Log a warning message indicating a potential misplaced method.
        Jvm.warn().on(getClass(),
                "Attempt to read method name/id but not at the start of a method, the previous method name was "
                        + lastEventName + "\n" + wireIn.bytes().toHexString(start, 1024));
        if (lastStart < start && lastStart + 1024 >= start)
            Jvm.warn().on(getClass(),
                    "The previous message was\n" + wireIn.bytes().toHexString(lastStart, start - lastStart));
    }

    /**
     * Handles binary wires that encode the field by numeric id. The id is read
     * as a stop-bit long. If a parselet is registered for that id it is invoked;
     * otherwise {@link #fieldNumberParselet} is called.
     *
     * @param wireIn binary wire to read from
     * @throws InvalidMarshallableException if the wire contains malformed data
     */
    private void parseOneBinary(@NotNull WireIn wireIn) throws InvalidMarshallableException {
        long methodId = wireIn.readEventNumber();

        // Check if methodId is mapped in the numberedConsumer.
        if (methodId == (int) methodId) {
            Map.Entry<String, WireParselet> entry = numberedConsumer.get((int) methodId);
            if (entry != null) {
                WireParselet parselet = entry.getValue();
                parselet.accept(entry.getKey(), wireIn.getValueIn());
                return;
            }
        }
        // If methodId isn't found, use the field number parselet to parse.
        fieldNumberParselet.readOne(methodId, wireIn);
    }

    /**
     * Registers {@code valueInConsumer} for both the text and numeric forms of
     * {@code key}.
     */
    @NotNull
    @Override
    public VanillaWireParser register(@NotNull WireKey key, WireParselet valueInConsumer) {
        return register(key.name().toString(), key.code(), valueInConsumer);
    }

    /**
     * Registers {@code valueInConsumer} to handle the field named {@code keyName}.
     * Also associates it with {@code keyName.hashCode()} for binary ids.
     *
     * @param keyName         textual field name
     * @param valueInConsumer parselet invoked for this field
     * @return this parser instance
     */
    @NotNull
    public VanillaWireParser register(String keyName, WireParselet valueInConsumer) {
        // Compute the hash code of the keyName and register.
        return register(keyName, keyName.hashCode(), valueInConsumer);
    }

    /**
     * Registers a WireParselet with a given keyName and code.
     * The keyName is stored in the namedConsumer map and the code
     * with its corresponding keyName in the numberedConsumer map.
     *
     * @param keyName         textual name of the field
     * @param code            numeric id of the field
     * @param valueInConsumer parselet invoked for this field
     * @return this parser instance
     */
    private VanillaWireParser register(String keyName, int code, WireParselet valueInConsumer) {
        // Store the WireParselet in the namedConsumer map using the keyName.
        namedConsumer.put(keyName, valueInConsumer);

        // Store the keyName and its WireParselet in the numberedConsumer map using the code.
        numberedConsumer.put(code, new AbstractMap.SimpleEntry<>(keyName, valueInConsumer));
        return this;
    }

    /**
     * Returns the parselet registered for {@code name} or {@code null} if none.
     */
    @Override
    public WireParselet lookup(CharSequence name) {
        return namedConsumer.get(name);
    }
}
