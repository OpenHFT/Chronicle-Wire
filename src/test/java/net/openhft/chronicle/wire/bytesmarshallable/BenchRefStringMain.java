package net.openhft.chronicle.wire.bytesmarshallable;

public class BenchRefStringMain {
    public static void main(String[] args) {
        PerfRegressionHolder main = new PerfRegressionHolder();
        main.doTest(main::benchRefString);
    }
}
