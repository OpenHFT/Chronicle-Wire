package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.DynamicEnum;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/*
 * Created by peter.lawrey@chronicle.software on 28/07/2017
 */
public class UnknownEnumTest {

    public Wire createWire() {
        return new TextWire(Bytes.elasticHeapByteBuffer(128));
    }

    @Test
    public void testUnknownEnum() {
        Wire wire = createWire();
        wire.write("value").text("Maybe");

        YesNo yesNo = wire.read("value").asEnum(YesNo.class);

        Wire wire2 = createWire();
        wire2.write("value").asEnum(yesNo);

        String maybe = wire2.read("value").text();
        assertEquals("Maybe", maybe);
    }

    @Test
    public void shouldGenerateFriendlyErrorMessageWhenTypeIsNotKnown() throws Exception {
        try {
            TextWire.from("enumField: !UnknownEnum QUX").
                    valueIn.wireIn().read("enumField").object();
            fail();
        } catch (Exception e) {
            assertThat(e.getMessage(),
                    is(equalTo("Could not determine type for value QUX of type !UnknownEnum {\n}\n")));
        }
    }

    enum YesNo implements DynamicEnum {
        Yes,
        No
    }
}
