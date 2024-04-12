package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

// Define a class representing a marshallable entity with a message field
public class MyClass extends SelfDescribingMarshallable {
    String msg; // Field to hold a message string
}
