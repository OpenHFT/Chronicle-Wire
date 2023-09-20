package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.util.NanoSampler;
import net.openhft.chronicle.jlbh.JLBH;
import net.openhft.chronicle.jlbh.JLBHOptions;
import net.openhft.chronicle.jlbh.JLBHTask;
import net.openhft.chronicle.jlbh.TeamCityHelper;

public class AbstractTimestampLongConverterJLBHBenchmark implements JLBHTask {

    // Constants for testing
    private static final long TIMESTAMP_MILLIS = 1675177572L;
    private static final String TIMESTAMP_STRING_SUFFIX = "1970-01-20T19:19:37.572+10:00";
    private static final String TIMESTAMP_STRING_NO_SUFFIX = "1970-01-20T09:19:37.572";
    private static final int ITERATIONS = 100_100;

    // Disabling JVM resource tracing
    static {
        System.setProperty("jvm.resource.tracing", "false");
    }

    // Converter instances for test scenarios
    private final LongConverter converterWithTimeZone = new MilliTimestampLongConverter("Australia/Melbourne");
    private final LongConverter converterUTC = new MilliTimestampLongConverter("UTC");
    private final StringBuilder sb = new StringBuilder();

    // JLBH-related fields for measuring performance
    private JLBH jlbh;
    private NanoSampler parseTZnoSuffix;
    private NanoSampler parseTZsuffix;
    private NanoSampler appendTZ;
    private NanoSampler parseUTCnoSuffix;
    private NanoSampler parseUTCsuffix;
    private NanoSampler appendUTC;

    // Main entry point to run the benchmark
    public static void main(String[] args) {
        new AbstractTimestampLongConverterJLBHBenchmark().run();
    }

    // Configure and initiate the JLBH benchmark
    public void run() {
        JLBHOptions jlbhOptions = new JLBHOptions()
                .iterations(ITERATIONS)
                .runs(5)
                .recordOSJitter(false)
                .accountForCoordinatedOmission(false)
                .warmUpIterations(10_000)
                .jlbhTask(this);
        JLBH jlbh = new JLBH(jlbhOptions);
        jlbh.start();
    }

    // Initializing JLBH probes for performance measurement
    @Override
    public void init(JLBH jlbh) {
        this.jlbh = jlbh;
        parseTZnoSuffix = jlbh.addProbe("parseTZnoSuffix");
        parseTZsuffix = jlbh.addProbe("parseTZsuffix");
        appendTZ = jlbh.addProbe("appendTZ");
        parseUTCnoSuffix = jlbh.addProbe("parseUTCnoSuffix");
        parseUTCsuffix = jlbh.addProbe("parseUTCsuffix");
        appendUTC = jlbh.addProbe("appendUTC");
    }

    // Benchmarking the performance of both converters with different scenarios
    @Override
    public void run(long startTimeNS) {
        // Testing converter with a timezone
        runTests(converterWithTimeZone, appendTZ, parseTZnoSuffix, parseTZsuffix);

        // Testing converter in UTC (without a suffix)
        runTests(converterUTC, appendUTC, parseUTCnoSuffix, parseUTCsuffix);

        jlbh.sampleNanos(System.nanoTime() - startTimeNS);
    }

    // Called when the benchmark completes
    @Override
    public void complete() {
        TeamCityHelper.teamCityStatsLastRun("LongConverterPerf", jlbh, ITERATIONS, System.out);
    }

    // Private utility function to run specific performance tests for a given converter
    private void runTests(LongConverter converter, NanoSampler appendSampler, NanoSampler parseNoSuffixSampler, NanoSampler parseSuffixSampler) {
        // append
        long testStartTime = System.nanoTime();
        sb.setLength(0);
        converter.append(sb, TIMESTAMP_MILLIS);
        appendSampler.sampleNanos(System.nanoTime() - testStartTime);

        // parse (without suffix)
        testStartTime = System.nanoTime();
        converter.parse(TIMESTAMP_STRING_NO_SUFFIX);
        parseNoSuffixSampler.sampleNanos(System.nanoTime() - testStartTime);

        // parse (with suffix)
        testStartTime = System.nanoTime();
        converter.parse(TIMESTAMP_STRING_SUFFIX);
        parseSuffixSampler.sampleNanos(System.nanoTime() - testStartTime);
    }
}
