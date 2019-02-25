package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.Marshallable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DeepCopyTest {
    protected static void doTest(AClass aClass) {
        AClass ac2 = aClass.deepCopy();
        assertEquals(aClass, ac2);
        String s = aClass.toString();
        assertEquals(s, ac2.toString());
        AClass ac3 = Marshallable.fromString(s);
        assertEquals(aClass, ac3);
    }

    @Test
    public void copyAClass() {
        AClass aClass = new AClass(1, true, (byte) 2, '3', (short) 4, 5, 6, 7, 8, "nine");
        doTest(aClass);
    }
}
