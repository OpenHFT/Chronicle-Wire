package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

public class MarshallableWithOverwriteFalseTest extends WireTestCommon {

    @Test
    public void test() {

        MyDto2 myDto2 = new MyDto2();
        MyDto myDto1 = new MyDto();

        myDto2.myDto.put("", myDto1);
        myDto1.strings.add("hello");
        myDto1.strings.add("world");

        String cs = myDto2.toString();
       // System.out.println(cs);
        MyDto2 o = (MyDto2) Marshallable.fromString(cs);

        assertEquals(2, o.myDto.get("").strings.size());
    }

    static class MyDto extends SelfDescribingMarshallable {
        List<String> strings = new ArrayList<>();

        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {

            // WORKS
             // Wires.readMarshallable(this, wire, true);  // WORKS

            // FAILS
            Wires.readMarshallable(this, wire, false);
        }
    }

    static class MyDto2 extends SelfDescribingMarshallable {
        Map<String, MyDto> myDto = new TreeMap<String, MyDto>();
    }
}
