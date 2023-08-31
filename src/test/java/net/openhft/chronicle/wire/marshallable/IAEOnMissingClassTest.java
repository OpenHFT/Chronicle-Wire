package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class IAEOnMissingClassTest {
    @Test(expected = IllegalArgumentException.class)
    public void throwIllegalArgumentExceptionOnMissingClassAlias() {
        Wire wire = new TextWire(Bytes.from("" +
                "a: !Aaa { hi: bye }"));
        Object object = wire.read("a").object();
        System.out.println(object);
        assertNotNull(object);
    }
}
