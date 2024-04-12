/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.channel.book;

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/* Windows 10 laptop, Intel(R) Core(TM) i9-10980HK CPU @ 2.40GHz

-Dthreads=4 -Dcount=5 -Dbatch=10 -Xmn24m
batch: 10
time: 23.124 sec
rate: 216.225 M objs/sec
avgLatency: 18 ns

 */

/**
 * This class is responsible for conducting a performance benchmark related to object allocations.
 * Specifically, it focuses on the allocation of 'TopOfBook' objects.
 */
public class PerfTopOfBookAllocationMain {

    // PROCS holds the number of available processors/threads in the system.
    static final int PROCS = Runtime.getRuntime().availableProcessors();

    // THREADS specifies the number of threads to be used in the benchmark, defaulting to PROCS.
    static int THREADS = Integer.getInteger("threads", PROCS);

    // COUNT determines the scale factor for the number of operations in the benchmark.
    static final int COUNT = Integer.getInteger("count", 5);

    // BATCH size is tuned to approximate real-world benchmark scenarios.
    static final int BATCH = Integer.getInteger("batch", 10);

    /**
     * Main method - entry point of the benchmark.
     * It utilizes different thread counts to observe how it impacts the performance of the allocation of TopOfBook objects.
     *
     * @param args Not utilized in this context.
     */
    public static void main(String[] args) {
        // Array representing different thread count scenarios for the benchmark.
        int[] threadArr = {1, 2, 3, 4, 6, 8, 12, 16, 24, 32};

        // If THREADS has been defined (not 0), override the threadArr to only use the specified number of threads.
        if (THREADS > 0)
            threadArr = new int[]{THREADS};

        // Loop over each thread count scenario and run the benchmark.
        for (int t : threadArr) {
            THREADS = t;
            main0();
        }
    }

    /**
     * Main0 conducts a single run of the benchmark with the current configuration (THREADS, COUNT, BATCH).
     * It measures allocation performance and outputs results such as batch size, time taken, rate, and average latency.
     */
    static void main0() {
        // Print current configuration to the console.
        System.out.println(""
                + "-Dthreads=" + THREADS + " "
                + "-Dcount=" + COUNT + " "
                + "-Dbatch=" + BATCH);

        // Capture the start time of the benchmark.
        long start = System.currentTimeMillis();

        // Calculate the total count by multiplying COUNT by 1,000,000,000.
        final long count = COUNT * 1_000_000_000L;
        final int batch = BATCH;

        // Conduct the benchmark: Allocate TopOfBook objects in parallel, in batches, and find the maximum batch size.
        final OptionalInt max =
                LongStream.range(0, THREADS)
                        .parallel()
                        .flatMap(i -> LongStream.range(0, count / batch / THREADS))
                        .mapToObj(l -> IntStream.range(0, batch)
                                .mapToObj(i -> new TopOfBook().sendingTimeNS(i))
                                .collect(Collectors.toList()))
                        .mapToInt(List::size)
                        .max();

        // Calculate the elapsed time of the benchmark.
        long time = System.currentTimeMillis() - start;

        // Print the benchmark results to the console.
        System.out.println("batch: " + max.getAsInt());
        System.out.println("time: " + time / 1e3 + " sec");
        System.out.println("rate: " + count / time / 1e3 + " M objs/sec");

        // Calculate and print the average latency of the allocations.
        long avgLatency = Math.min(THREADS, PROCS) * time * 1_000_000 / count;
        System.out.println("avgLatency: " + avgLatency + " ns");
    }
}
