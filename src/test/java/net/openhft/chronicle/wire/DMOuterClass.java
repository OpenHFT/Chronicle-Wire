package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class DMOuterClass extends SelfDescribingMarshallable {
    String text;
    boolean b;
    byte bb;
    short s;
    float f;
    double d;
    long l;
    int i;
    @NotNull
    List<DMNestedClass> nested = new ArrayList<>();
    @NotNull
    Map<String, DMNestedClass> map = new LinkedHashMap<>();

    DMOuterClass() {

    }

    public DMOuterClass(String text, boolean b, byte bb, double d, float f, int i, long l, short s) {
        this.text = text;
        this.b = b;
        this.bb = bb;
        this.d = d;
        this.f = f;
        this.i = i;
        this.l = l;
        this.s = s;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public String toString() {
        return  "text: " + text + ",\n" +
                "b: " + text + ",\n" +
                "bb: " + text + ",\n" +
                "d: " + text + ",\n" +
                "f: " + text + ",\n" +
                "i: " + text + ",\n" +
                "l: " + text + ",\n" +
                "s: " + text + ",\n" +
                "nested: " + nested.toString() + ",\n" +
                "map: " + map.toString();
    }
}
