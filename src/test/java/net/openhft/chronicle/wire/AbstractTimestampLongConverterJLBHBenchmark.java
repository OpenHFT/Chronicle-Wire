package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.util.NanoSampler;
import net.openhft.chronicle.jlbh.JLBH;
import net.openhft.chronicle.jlbh.JLBHOptions;
import net.openhft.chronicle.jlbh.JLBHTask;
import net.openhft.chronicle.jlbh.TeamCityHelper;

public class AbstractTimestampLongConverterJLBHBenchmark implements JLBHTask {

    private static final long TIMESTAMP_MILLIS = 1675177572L;
    private static final String TIMESTAMP_STRING_SUFFIX = "1970-01-20T19:19:37.572+10:00";
    private static final String TIMESTAMP_STRING_NO_SUFFIX = "1970-01-20T09:19:37.572";
    private static final int ITERATIONS = 100_100;

    static {
        System.setProperty("jvm.resource.tracing", "false");
    }

    private final LongConverter converterWithTimeZone = new MilliTimestampLongConverter("Australia/Melbourne");
    private final LongConverter converterUTC = new MilliTimestampLongConverter("UTC");
    private final StringBuilder sb = new StringBuilder();
    private JLBH jlbh;
    private NanoSampler parseTZnoSuffix;
    private NanoSampler parseTZsuffix;
    private NanoSampler appendTZ;
    private NanoSampler parseUTCnoSuffix;
    private NanoSampler parseUTCsuffix;
    private NanoSampler appendUTC;

    public static void main(String[] args) {
        new AbstractTimestampLongConverterJLBHBenchmark().run();
    }

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

    @Override
    public void run(long startTimeNS) {
        /*
            Converter with timezone
         */
        runTests(converterWithTimeZone, appendTZ, parseTZnoSuffix, parseTZsuffix);

        /*
            Converter in UTC (no suffix)
         */
        runTests(converterUTC, appendUTC, parseUTCnoSuffix, parseUTCsuffix);

        jlbh.sampleNanos(System.nanoTime() - startTimeNS);
    }

    @Override
    public void complete() {
        TeamCityHelper.teamCityStatsLastRun("LongConverterPerf", jlbh, ITERATIONS, System.out);
    }

    private void runTests(LongConverter converter, NanoSampler appendSampler, NanoSampler parseNoSuffixSampler, NanoSampler parseSuffixSampler) {
        // append
        long testStartTime = System.nanoTime();
        sb.setLength(0);
        converter.append(sb, TIMESTAMP_MILLIS);
        appendSampler.sampleNanos(System.nanoTime() - testStartTime);

        // parse (no suffix)
        testStartTime = System.nanoTime();
        converter.parse(TIMESTAMP_STRING_NO_SUFFIX);
        parseNoSuffixSampler.sampleNanos(System.nanoTime() - testStartTime);

        // parse (suffix)
        testStartTime = System.nanoTime();
        converter.parse(TIMESTAMP_STRING_SUFFIX);
        parseSuffixSampler.sampleNanos(System.nanoTime() - testStartTime);
    }
}
