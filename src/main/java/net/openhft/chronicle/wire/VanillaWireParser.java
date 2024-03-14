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
 * Provides an implementation of the WireParser interface, parsing wire inputs using both named and numbered
 * parselets to associate specific actions with events or field names.
 * <p>
 * This parser uses a default consumer to handle unmatched entries and a field number parselet for numbered fields.
 * </p>
 */
public class VanillaWireParser implements WireParser {

    // Map of field names to their associated parselets, sorted by CharSequence order.
    private final Map<CharSequence, WireParselet> namedConsumer = new TreeMap<>(CharSequenceComparator.INSTANCE);

    // Map of field numbers to their associated parselets.
    private final Map<Integer, Map.Entry<String, WireParselet>> numberedConsumer = new HashMap<>();

    // The default consumer to handle unmatched entries.
    private final WireParselet defaultConsumer;

    // Used for building strings for parsing.
    private final StringBuilder sb = new StringBuilder(128);

    // Holds the name of the last event that was parsed.
    private final StringBuilder lastEventName = new StringBuilder(128);

    // Handles numbered fields.
    private FieldNumberParselet fieldNumberParselet;

    // Holds the last parslet that was used.
    private WireParselet lastParslet = null;

    // Indicates the position in the wire input of the last parsed event.
    private long lastStart = 0;

    /**
     * Constructs a new VanillaWireParser with the specified default consumer and field number parselet.
     *
     * @param defaultConsumer The default consumer to handle unmatched entries.
     * @param fieldNumberParselet The field number parselet for handling numbered fields.
     */
    public VanillaWireParser(@NotNull WireParselet defaultConsumer,
                             @NotNull FieldNumberParselet fieldNumberParselet) {
        this.defaultConsumer = defaultConsumer;

        // Initializing the lastEventName with a non-ASCII value to ensure uniqueness.
        lastEventName.appendCodePoint(0xFFFF);
        this.fieldNumberParselet = fieldNumberParselet;
    }

    /**
     * Peeks at the next code or byte in the wire input without moving the read position.
     *
     * @param wireIn The wire input to peek into.
     * @return The next unsigned byte as an integer.
     */
    private int peekCode(@NotNull WireIn wireIn) {
        return wireIn.bytes().peekUnsignedByte();
    }

    @Override
    public WireParselet getDefaultConsumer() {
        return defaultConsumer;
    }

    /**
     * Parses a single input from the wire. Determines if the input should be parsed as binary or not.
     * Throws specific exceptions in the case of invocation issues or invalid marshallable data.
     *
     * @param wireIn The wire input to parse.
     * @throws InvocationTargetRuntimeException if an invocation error occurs during parsing.
     * @throws InvalidMarshallableException if invalid marshallable data is encountered during parsing.
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
     * Handles the scenario where an attempt to read a method name results in an empty value.
     * Logs a warning about the situation and the contents leading up to this.
     *
     * @param wireIn The wire input being parsed.
     * @param start  The position in the wire input at which the method started.
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
     * Parses binary data from the wire based on a method ID. If the ID is not mapped, the
     * field number parselet is used to continue the parsing process.
     *
     * @param wireIn The wire input containing binary data.
     * @throws InvalidMarshallableException if invalid marshallable data is encountered.
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

    @NotNull
    @Override
    public VanillaWireParser register(@NotNull WireKey key, WireParselet valueInConsumer) {
        return register(key.name().toString(), key.code(), valueInConsumer);
    }

    /**
     * Registers a WireParselet with a keyName. This method calculates the hashcode
     * of the keyName and delegates to the private register method.
     *
     * @param keyName         The name of the key to register.
     * @param valueInConsumer The WireParselet associated with the keyName.
     * @return Returns the current instance of VanillaWireParser for method chaining.
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
     * @param keyName         The name of the key to register.
     * @param code            The code associated with the keyName.
     * @param valueInConsumer The WireParselet associated with the keyName.
     * @return Returns the current instance of VanillaWireParser for method chaining.
     */
    private VanillaWireParser register(String keyName, int code, WireParselet valueInConsumer) {
        // Store the WireParselet in the namedConsumer map using the keyName.
        namedConsumer.put(keyName, valueInConsumer);

        // Store the keyName and its WireParselet in the numberedConsumer map using the code.
        numberedConsumer.put(code, new AbstractMap.SimpleEntry<>(keyName, valueInConsumer));
        return this;
    }

    @Override
    public WireParselet lookup(CharSequence name) {
        return namedConsumer.get(name);
    }
}
