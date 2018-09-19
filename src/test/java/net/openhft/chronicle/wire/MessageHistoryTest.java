package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/*
 * Created by jerry on 19/06/17.
 */
public class MessageHistoryTest {

    @Test
    public void checkHistoryMaxSizeException() {
        VanillaMessageHistory container1 = new VanillaMessageHistory();
        container1.addSourceDetails(true);
        VanillaMessageHistory container2 = new VanillaMessageHistory();
        container2.addSourceDetails(true);
        for (int i = 0; i < VanillaMessageHistory.MESSAGE_HISTORY_LENGTH / 2; i++) {
            Wires.copyTo(container1, container2);
            Wires.copyTo(container2, container1);
        }
        try {
            Wires.copyTo(container1, container2);
            fail();
        } catch (IllegalStateException e) {
            // all good
        }
    }

    @Test
    public void checkToString() {
        VanillaMessageHistory container1 = new VanillaMessageHistory();
        container1.addSourceDetails(true);
        container1.addSource(1, 0xff);
        container1.addSource(2, 0xfff);
        container1.addTiming(10_000);
        container1.addTiming(20_000);
        assertEquals(2, container1.sources());
        assertEquals(2, container1.timings());
        System.out.println(container1.toString());
        assertEquals(2, container1.sources());
        assertEquals(2, container1.timings());
    }
}
