package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

// Test created as a result of agitator tests i.e. random character changes
public class TextWireAgitatorTest {
    @Test(expected = IORuntimeException.class)
    public void lowerCaseClass() {
        TextWireTest.MyDto myDto = Marshallable.fromString("!" + TextWireTest.MyDto.class.getName() + " { }");
        assertEquals("!net.openhft.chronicle.wire.TextWireTest$MyDto {\n" +
                "  strings: [\n" +
                "  ]\n" +
                "}\n", myDto.toString());

        TextWireTest.MyDto myDto2 = Marshallable.fromString("!" + TextWireTest.MyDto.class.getName().toLowerCase() + " { }");
    }
}
