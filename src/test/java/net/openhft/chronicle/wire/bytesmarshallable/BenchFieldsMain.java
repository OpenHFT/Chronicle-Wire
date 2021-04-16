package net.openhft.chronicle.wire.bytesmarshallable;

public class BenchFieldsMain {
    public static void main(String[] args) {
        PerfRegressionHolder main = new PerfRegressionHolder();
        main.doTest(main::benchFields);
    }
}
