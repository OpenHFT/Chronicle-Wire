package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

// Test created as a result of agitator tests i.e. random character changes
public class TextWireAgitatorTest extends WireTestCommon {

    @Test
    public void lowerCaseClass() {
        assertFalse(OS.isWindows());
        expectException("Unable to load net.openhft.chronicle.wire.textwiretest$mydto, is a class alias missing");
        assertTrue(Marshallable.fromString("!" + TextWireTest.MyDto.class.getName().toLowerCase() + " { }") instanceof Map);
    }

    @Test(expected = IORuntimeException.class)
    public void colonInList() {
        TextWireTest.MyDto md = Marshallable.fromString("!net.openhft.chronicle.wire.TextWireTest$MyDto {\n" +
                "  strings: [\n" +
                "  :\n" +
                "  ]\n" +
                "}\n");
        assertEquals("[hello]", md.toString());
    }

    @Test
    public void notBoolean() {
        // produces a warning.
        MyFlagged mf = Marshallable.fromString("!net.openhft.chronicle.wire.TextWireAgitatorTest$MyFlagged {\n" +
                "  flag: not-false\n" +
                "}");
        assertNotNull(mf);
    }

    static class MyFlagged extends SelfDescribingMarshallable {
        boolean flag;
    }
}
