package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.NativeBytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LongArrayDirectReferenceTest {
    @Test
    public void getSetValues() {
        int length = 1024 + 8;
        try (NativeBytes bytes = NativeBytes.nativeBytes(length)) {
            LongArrayDirectReference.write(bytes, 128);

            LongArrayDirectReference array = new LongArrayDirectReference();
            array.bytesStore(bytes, 0, length);

            assertEquals(128, array.getCapacity());
            for (int i = 0; i < 128; i++)
                array.setValueAt(i, i + 1);

            for (int i = 0; i < 128; i++)
                assertEquals(i + 1, array.getValueAt(i));
        }
    }

}