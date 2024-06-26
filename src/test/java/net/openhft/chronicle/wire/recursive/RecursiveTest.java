package net.openhft.chronicle.wire.recursive;

import net.openhft.chronicle.wire.WireMarshaller;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test for recursion in the marshaller and fields. WIRE_MARSHALLER_CL.get should not recurse while
 * looking up fields of the class. At time of writing this occurs when checking if the component class
 * of a subfield is a leaf and is only a problem when the component class is the same as the parent class.
 */
public class RecursiveTest {

    @Test
    public void referToBaseClass() {
        test(new ReferToBaseClass("hello"), new ReferToBaseClass(null));
    }

    @Test
    public void referToSameClass() {
        test(new ReferToSameClass("test"), new ReferToSameClass(null));
    }

    @Test
    public void marshallerReferToSameClass() {
        WireMarshaller<?> marshaller= WireMarshaller.WIRE_MARSHALLER_CL.get(ReferToSameClass.class);
        assertNotNull(marshaller);
    }

    @Test
    public void marshallerReferToBaseClass() {
        WireMarshaller<?> marshaller = WireMarshaller.WIRE_MARSHALLER_CL.get(ReferToBaseClass.class);
        assertNotNull(marshaller);
    }

    private void test(Base from, Base to) {
        from.copyTo(to);
        assertEquals(from.name(), to.name());
    }
}