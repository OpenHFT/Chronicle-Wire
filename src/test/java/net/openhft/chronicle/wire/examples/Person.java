/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.examples;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.converter.NanoTime;
import net.openhft.chronicle.wire.converter.ShortText;

/**
 * Represents a person with example attributes.
 * Uses {@link SelfDescribingMarshallable} for wire-friendly serialisation.
 */
public class Person extends SelfDescribingMarshallable {

    private String name;

    @NanoTime
    private long timestampNS;
    @ShortText
    private long userName;

    /**
     * Return the person name used in example payloads.
     *
     * @return the person name as stored
     */
    public String name() {
        return name;
    }

    /**
     * Update the person name used in example payloads.
     *
     * @param name person name to store
     * @return this person instance for method chaining
     */
    public Person name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Return the timestamp in nanoseconds stored via {@link NanoTime}.
     *
     * @return timestamp in nanoseconds
     */
    public long timestampNS() {
        return timestampNS;
    }

    /**
     * Set the timestamp in nanoseconds for {@link NanoTime} encoding.
     *
     * @param timestampNS timestamp in nanoseconds
     * @return this person instance for method chaining
     */
    public Person timestampNS(long timestampNS) {
        this.timestampNS = timestampNS;
        return this;
    }

    /**
     * Return the short-text user name token stored for this person.
     *
     * @return short-text user name token
     */
    public long userName() {
        return userName;
    }

    /**
     * Set the short-text user name token for {@link ShortText} encoding.
     *
     * @param userName short-text user name token to store
     * @return this person instance for method chaining
     */
    public Person userName(long userName) {
        this.userName = userName;
        return this;
    }
}
