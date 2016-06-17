package net.openhft.chronicle.wire;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Rob Austin.
 */
public class ProjectTest {

    static class Dto1 extends AbstractMarshallable {
        Map m = new HashMap<>();
        String anotherField;
        long someValue;
    }

    static class Dto2 extends AbstractMarshallable {
        long someValue;
        String anotherField;
        Map m = new HashMap<>();
    }


    @Test
    public void testProject() throws Exception {
        Dto1 dto1 = new Dto1();
        dto1.m.put("some", "data");
        dto1.anotherField = "someString";
        dto1.someValue = 1;


        Dto2 dto2 = Wires.project(Dto2.class, dto1);

        Assert.assertEquals(dto2.someValue, dto1.someValue);
        Assert.assertEquals(dto2.anotherField, dto1.anotherField);
        Assert.assertEquals(dto2.m, dto1.m);

    }
}
