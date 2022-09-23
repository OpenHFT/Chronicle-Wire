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

/**
 * Benchmark for just allocations
 */
public class PerfTopOfBookAllocationMain {
    static final int PROCS = Runtime.getRuntime().availableProcessors();
    static final int THREADS = Integer.getInteger("threads", PROCS);
    static final int COUNT = Integer.getInteger("count", 2);
    // adjusted to approximate what is seen in more realistic benchmarks
    static final int BATCH = Integer.getInteger("batch", 10);

    public static void main(String[] args) {
        System.out.println(""
                + "-Dthreads=" + THREADS + " "
                + "-Dcount=" + COUNT + " "
                + "-Dbatch=" + BATCH);
        long start = System.currentTimeMillis();
        final long count = COUNT * 1_000_000_000L;
        final int batch = BATCH;
        final OptionalInt max =
                LongStream.range(0, THREADS)
                        .parallel()
                        .flatMap(i -> LongStream.range(0, count / batch / THREADS))
                        .mapToObj(l -> IntStream.range(0, batch)
                                .mapToObj(i -> new TopOfBook().sendingTimeNS(i))
                                .collect(Collectors.toList()))
                        .mapToInt(List::size)
                        .max();
        long time = System.currentTimeMillis() - start;
        System.out.println("batch: " + max.getAsInt());
        System.out.println("time: " + time / 1e3 + " sec");
        System.out.println("rate: " + count / time / 1e3 + " M objs/sec");
        long avgLatency = Math.min(THREADS, PROCS) * time * 1_000_000 / count;
        System.out.println("avgLatency: " + avgLatency + " ns");
    }
}
