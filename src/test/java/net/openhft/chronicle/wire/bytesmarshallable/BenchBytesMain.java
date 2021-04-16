package net.openhft.chronicle.wire.bytesmarshallable;

public class BenchBytesMain {
    public static void main(String[] args) {
        PerfRegressionHolder main = new PerfRegressionHolder();
        main.doTest(main::benchBytes);
    }
}
