package run.chronicle.wire.perf;

import net.openhft.affinity.AffinityLock;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.util.NanoSampler;
import net.openhft.chronicle.jlbh.JLBH;
import net.openhft.chronicle.jlbh.JLBHOptions;
import net.openhft.chronicle.jlbh.JLBHTask;
import net.openhft.chronicle.jlbh.TeamCityHelper;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * This runs a JLBH test and triggers the async profiler to record each run after the warmup is completed
 * <p>
 *     The async profiler can be downloaded from <a href=https://github.com/async-profiler/async-profiler>here</a>
 * </p>
 * <p>
 *     <li>
 *     The <code>profiler.location</code> system property should be set to the directory of the async profiler executable
 *     </li>
 *     <li>
 *     The <code>profiler.sampleIntervalNanos</code> system property should be set to the desired sampling interval in nanoseconds.
 *     </li>
 *     <li>
 *     The <code>jlbh.runs</code> system property can be set to the number of runs to profile.
 *     </li>
 * </p>
 */
public class JSONWireMultiLayeredPerfJLBH implements JLBHTask {

    private static final int ITERATIONS = 1_000_000;

    static {
        System.setProperty("jvm.resource.tracing", "false");
        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(false);
    }

    public static void main(String[] args) {
        String profilerLocation = System.getProperty("profiler.location");
        int sampleIntervalNanos = Integer.getInteger("profiler.sampleIntervalNanos", 10);
        int runs = Integer.getInteger("jlbh.runs", 1);
        JSONWireMultiLayeredPerfJLBH benchmark = new JSONWireMultiLayeredPerfJLBH(profilerLocation, sampleIntervalNanos, runs);
        JLBHOptions jlbhOptions = new JLBHOptions()
                .iterations(ITERATIONS)
                .throughput(100_000)
                .runs(benchmark.runsTotal)
                .recordOSJitter(false)
                .accountForCoordinatedOmission(true)
                .warmUpIterations(10_000)
                .acquireLock(AffinityLock::acquireCore)
                .jlbhTask(benchmark);
        JLBH jlbh = new JLBH(jlbhOptions);
        jlbh.start();
    }


    private MultiLayeredExample singleLayer = new MultiLayeredExample();

    private MultiLayeredExample doubleLayer = new MultiLayeredExample();
    private MultiLayeredExample tripleLayer = new MultiLayeredExample();
    private NanoSampler singleLayerWriteSample;
    private NanoSampler doubleLayerWriteSample;
    private NanoSampler tripleLayerWriteSample;
    private NanoSampler singleLayerReadSample;
    private NanoSampler doubleLayerReadSample;
    private NanoSampler tripleLayerReadSample;
    private NanoSampler singleLayerToStringSample;

    private NanoSampler doubleLayerToStringSample;
    private NanoSampler tripleLayerToStringSample;
    private NanoSampler singleLayerFromStringSample;
    private NanoSampler doubleLayerFromStringSample;
    private NanoSampler tripleLayerFromStringSample;
    private JLBH jlbh;

    private String profilerStartCall;
    private IntFunction<String> profilerEndCall;
    private Process profiler;

    private int runsTotal;
    private int runNumber = 0;
    private static int pid = OS.getProcessId();

    public JSONWireMultiLayeredPerfJLBH(String profilerLocation, int sampleIntervalNanos, int runs) {
        this.runsTotal = runs;
        if (profilerLocation == null || profilerLocation.isEmpty() || OS.isWindows()) {
            profilerStartCall = null;
        } else {
            profilerStartCall = profilerLocation + File.separator + "asprof start" +
                    " -i " + sampleIntervalNanos +
                    " -e cpu " +
                    pid;
            profilerEndCall = run -> profilerLocation + File.separator + "asprof stop" +
                    " -o jfr" +
                    " -f json_jlbh_run_" + run + ".jfr " + pid;
        }
    }

    private Wire jsonWire = WireType.JSON.apply(Bytes.elasticByteBuffer());

    @Override
    public void init(JLBH jlbh) {
        this.jlbh = jlbh;
        // single layer
        createSingleLayerExample();
        singleLayerWriteSample = jlbh.addProbe("singleLayerWrite");
        singleLayerReadSample = jlbh.addProbe("singleLayerRead");
        singleLayerToStringSample = jlbh.addProbe("singleLayerToString");
        singleLayerFromStringSample = jlbh.addProbe("singleLayerFromString");

        System.out.println("Single Layer: " + WireType.JSON.asString(singleLayer));

        // double layer
        createDoubleLayerExample();
        doubleLayerWriteSample = jlbh.addProbe("doubleLayerWrite");
        doubleLayerReadSample = jlbh.addProbe("doubleLayerRead");
        doubleLayerToStringSample = jlbh.addProbe("doubleLayerToString");
        doubleLayerFromStringSample = jlbh.addProbe("doubleLayerFromString");

        System.out.println("Double Layer: " + WireType.JSON.asString(doubleLayer));

        // triple layer
        createTripleLayerExample();
        tripleLayerWriteSample = jlbh.addProbe("tripleLayerWrite");
        tripleLayerReadSample = jlbh.addProbe("tripleLayerRead");
        tripleLayerToStringSample = jlbh.addProbe("tripleLayerToString");
        tripleLayerFromStringSample = jlbh.addProbe("tripleLayerFromString");

        System.out.println("Triple Layer: " + WireType.JSON.asString(tripleLayer));
    }

    private void createSingleLayerExample() {
        singleLayer.b = 1;
        singleLayer.s = 2;
        singleLayer.i = 3;
        singleLayer.l = 4;
        singleLayer.f = 5;
        singleLayer.d = 6;
        singleLayer.bool = true;
        singleLayer.text = "layer1";
        singleLayer.textList.add("single");
        // empty layered list and map
    }

    private void createDoubleLayerExample() {
        doubleLayer.b = 11;
        doubleLayer.s = 12;
        doubleLayer.i = 13;
        doubleLayer.l = 14;
        doubleLayer.f = 15;
        doubleLayer.d = 16;
        doubleLayer.bool = true;
        doubleLayer.text = "layer2";
        doubleLayer.textList.add("single");
        doubleLayer.textList.add("double");
        doubleLayer.example = singleLayer;
        doubleLayer.exampleMap.put("single", singleLayer);
    }

    private void createTripleLayerExample() {
        tripleLayer.b = 21;
        tripleLayer.s = 22;
        tripleLayer.i = 23;
        tripleLayer.l = 24;
        tripleLayer.f = 25;
        tripleLayer.d = 26;
        tripleLayer.bool = true;
        tripleLayer.text = "layer3";
        tripleLayer.textList.add("single");
        tripleLayer.textList.add("double");
        tripleLayer.textList.add("triple");
        tripleLayer.example = doubleLayer;
        tripleLayer.exampleMap.put("single", singleLayer);
        tripleLayer.exampleMap.put("double", doubleLayer);
    }

    @Override
    public void run(long startTimeNS) {
        // single layer
        marshallTest(singleLayer, singleLayerWriteSample, singleLayerReadSample);
        stringTest(singleLayer, singleLayerToStringSample, singleLayerFromStringSample);

        // double layer
        marshallTest(doubleLayer, doubleLayerWriteSample, doubleLayerReadSample);
        stringTest(doubleLayer, doubleLayerToStringSample, doubleLayerFromStringSample);

        // triple layer
        marshallTest(tripleLayer, tripleLayerWriteSample, tripleLayerReadSample);
        stringTest(tripleLayer, tripleLayerToStringSample, tripleLayerFromStringSample);
        jlbh.sampleNanos(System.nanoTime() - startTimeNS);
    }

    private void marshallTest(MultiLayeredExample example, NanoSampler writeSampler, NanoSampler readSampler) {
        jsonWire.clear();
        long start, end;

        start = System.nanoTime();
        example.writeMarshallable(jsonWire);
        end = System.nanoTime();
        writeSampler.sampleNanos(end - start);

        start = System.nanoTime();
        example.readMarshallable(jsonWire);
        end = System.nanoTime();
        readSampler.sampleNanos(end - start);
    }

    private void stringTest(MultiLayeredExample example, NanoSampler writeSampler, NanoSampler readSampler) {
        long start, end;

        start = System.nanoTime();
        String json = WireType.JSON.asString(example);
        end = System.nanoTime();
        writeSampler.sampleNanos(end - start);

        start = System.nanoTime();
        WireType.JSON.fromString(MultiLayeredExample.class, json);
        end = System.nanoTime();
        readSampler.sampleNanos(end - start);
    }

    @Override
    public void warmedUp() {
        // attach async profiler
        beforeRun();
    }

    private void beforeRun() {
        runNumber++;
        if (profilerStartCall != null) {
            try {
                profiler = Runtime.getRuntime().exec(profilerStartCall);
                System.out.println("Started profiler for run " + runNumber + " with call: " + profilerStartCall);
            } catch (IOException e) {
                System.err.println("Failed to start profiler: " + e.getMessage());
            }
        }
    }

    @Override
    public void runComplete() {
        if (profiler != null) {
            try {
                String profilerEnd = profilerEndCall.apply(runNumber);
                Process profilerProcess = Runtime.getRuntime().exec(profilerEnd);
                System.out.println("Stopped profiler for run " + runNumber + " with call: " + profilerEnd);
                profilerProcess.waitFor();
                profiler.waitFor();
            } catch (Exception e) {
                System.err.println("Failed to stop profiler: " + e.getMessage());
            }
        }

        if (runNumber < runsTotal) {
            System.out.println("Run " + runNumber + " complete");
            beforeRun();
        }
    }

    @Override
    public void complete() {
        TeamCityHelper.teamCityStatsLastRun(this.getClass().getSimpleName(), jlbh, ITERATIONS, System.out);
    }

    /**
     * An example class which covers all primative types plus a list of Strings and a map of Strings to Example objects.
     */
    public static class MultiLayeredExample extends SelfDescribingMarshallable {
        byte b;
        short s;
        int i;
        long l;
        float f;
        double d;
        boolean bool = true;
        String text = "example";
        List<String> textList = new ArrayList<>();
        MultiLayeredExample example = null;
        Map<String, MultiLayeredExample> exampleMap = new HashMap<>();
    }
}
