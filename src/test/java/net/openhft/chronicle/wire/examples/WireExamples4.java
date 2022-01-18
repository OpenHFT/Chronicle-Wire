package net.openhft.chronicle.wire.examples;

import net.openhft.chronicle.wire.JSONWire;
import net.openhft.chronicle.wire.Wire;

public class WireExamples4 {

    interface Printer {
        void print(String message);
    }

    public static void main(String[] args) {
        Wire wire = new JSONWire();
        final Printer printer = wire.methodWriter(Printer.class);
        printer.print("hello world");
        System.out.println(wire.bytes());
        wire.methodReader((Printer) System.out::println).readOne();
    }
}
