package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.Wires;
import org.junit.Assert;
import org.junit.Test;

public class Issue344Test extends WireTestCommon {
    @Test
    public void testFFFF() {
        runWith('\uFFFF');
    }

    @Test
    public void testFFFE() {
        runWith('\uFFFE');
    }

    private void runWith(char test) {
        final TestData data = new TestData();
        data.testChar = test;
        final TestData copyData = new TestData();
        Wires.copyTo(data, copyData);
        Assert.assertEquals(data.testChar, copyData.testChar);
    }

    private static class TestData implements Marshallable {
        public char testChar;
    }
}
