package net.openhft.chronicle.wire.examples;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.converter.NanoTime;
import net.openhft.chronicle.wire.converter.ShortText;

/**
 * Represents a person with specific attributes. This class inherits from
 * the SelfDescribingMarshallable which provides serialization functionality.
 */
public class Person extends SelfDescribingMarshallable {

    // Represents the name of the person.
    private String name;

    // Represents the timestamp in nanoseconds.
    @NanoTime
    private long timestampNS;
    @ShortText
    private long userName;

    /**
     * Retrieve the name of the person.
     *
     * @return The name of the person.
     */
    public String name() {
        return name;
    }

    /**
     * Set the name of the person.
     *
     * @param name The name to set.
     * @return This person instance, allowing for method chaining.
     */
    public Person name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Retrieve the timestamp in nanoseconds.
     *
     * @return The timestamp in nanoseconds.
     */
    public long timestampNS() {
        return timestampNS;
    }

    /**
     * Set the timestamp in nanoseconds.
     *
     * @param timestampNS The timestamp to set.
     * @return This person instance, allowing for method chaining.
     */
    public Person timestampNS(long timestampNS) {
        this.timestampNS = timestampNS;
        return this;
    }

    /**
     * Retrieve the username of the person.
     *
     * @return The username of the person.
     */
    public long userName() {
        return userName;
    }

    /**
     * Set the username of the person.
     *
     * @param userName The username to set.
     * @return This person instance, allowing for method chaining.
     */
    public Person userName(long userName) {
        this.userName = userName;
        return this;
    }
}
