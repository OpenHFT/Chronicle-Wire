package net.openhft.chronicle.wire.bytesmarshallable;

public class BenchArrayStringMain {
    public static void main(String[] args) {
        PerfRegressionHolder main = new PerfRegressionHolder();
        main.doTest(main::benchArrayString);
    }
}
