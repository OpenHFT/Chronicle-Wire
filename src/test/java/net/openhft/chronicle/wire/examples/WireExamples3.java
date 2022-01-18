package net.openhft.chronicle.wire.examples;

public class WireExamples3 {

    interface Printer {
        void print(String message);
    }


    public static void main(String[] args) {
        final Printer consolePrinter = System.out::println;
        consolePrinter.print("hello world");
    }
}
