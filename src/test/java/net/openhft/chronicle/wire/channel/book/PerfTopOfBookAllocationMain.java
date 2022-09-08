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
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        final long count = 3_000_000_000L;
        final int batch = 10;
        final OptionalInt max = LongStream.range(0, count / batch)
                .parallel()
                .mapToObj(l -> IntStream.range(0, batch)
                        .mapToObj(i -> new TopOfBook().sendingTimeNS(i))
                        .collect(Collectors.toList()))
                .mapToInt(List::size)
                .max();
        long time = System.currentTimeMillis() - start;
        System.out.println("batch: " + max.getAsInt());
        System.out.println("time: " + time / 1e3 + " sec");
        System.out.println("rate: " + count / time / 1e3 + " M objs/sec");
        long avgLatency = Runtime.getRuntime().availableProcessors() * time * 1_000_000 / count;
        System.out.println("avgLatency: " + avgLatency + " ns");
    }
}
