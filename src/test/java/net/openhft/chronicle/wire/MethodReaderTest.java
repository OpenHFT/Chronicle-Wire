package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

interface MockMethods {
    void method1(MockDto dto);

    void method2(MockDto dto);
}

/**
 * Created by peter on 17/05/2017.
 */
public class MethodReaderTest {
    @Test
    public void readMethods() throws IOException {
        Wire wire = new TextWire(BytesUtil.readFile("methods-in.yaml"));
        Wire wire2 = new TextWire(Bytes.allocateElasticDirect());
        // expected
        Bytes expected = BytesUtil.readFile("methods-in.yaml");
        MockMethods writer = wire2.methodWriter(MockMethods.class);
        MethodReader reader = wire.methodReader(writer);
        for (int i = 0; i < 2; i++) {
            assertTrue(reader.readOne());
            while (wire2.bytes().peekUnsignedByte(wire2.bytes().writePosition() - 1) == ' ')
                wire2.bytes().writeSkip(-1);
            wire2.bytes().append("---\n");
        }
        assertFalse(reader.readOne());
        assertEquals(expected.toString().trim().replace("\r", ""), wire2.toString().trim());
    }
}

class MockDto extends AbstractMarshallable {
    String field1;
    double field2;
}
