package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

// Class to test behavior when a missing class alias is encountered
public class IAEOnMissingClassTest {

    // Test to ensure that a missing class alias results in an IllegalArgumentException
    @Test(expected = IllegalArgumentException.class)
    public void throwIllegalArgumentExceptionOnMissingClassAlias() {

        // Create a wire object with a missing class alias 'Aaa'
        Wire wire = new TextWire(Bytes.from("" +
                "a: !Aaa { hi: bye }"));

        // Attempt to read the object from the wire
        Object object = wire.read("a").object();

        // Print the resulting object for debug purposes
        System.out.println(object);

        // Ensure that an object is returned (this check might be redundant given the expected exception)
        assertNotNull(object);
    }
}
