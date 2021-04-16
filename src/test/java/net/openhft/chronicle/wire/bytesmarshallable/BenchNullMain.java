package net.openhft.chronicle.wire.bytesmarshallable;

public class BenchNullMain {
    public static void main(String[] args) {
        PerfRegressionHolder main = new PerfRegressionHolder();
        main.doTest(main::benchNull);
    }
}
