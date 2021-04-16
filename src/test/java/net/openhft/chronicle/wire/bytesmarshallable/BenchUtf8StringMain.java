package net.openhft.chronicle.wire.bytesmarshallable;

public class BenchUtf8StringMain {
    public static void main(String[] args) {
        PerfRegressionHolder main = new PerfRegressionHolder();
        main.doTest(main::benchUtf8String);
    }
}
