/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xerial.snappy.Snappy;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/*
 * Created by Peter Lawrey on 01/12/15.
 */
public class SnappyComparisonMain {
    @Nullable
    static volatile byte[] blackHole = null;

    public static void main(String... args) throws IOException {
        test(100);
        for (int i = 100; i <= 1 << 20; i *= 2) {
            test(i);
        }
    }

    private static void test(int length) throws IOException {
        @NotNull StringBuilder sb = new StringBuilder();
        while (sb.length() < length)
            sb.append(Integer.toBinaryString(sb.length()));
        @NotNull String s = sb.toString();
        System.out.println(s.length() + " compressed to " + Snappy.compress(s).length);
        int saved = s.length() - Snappy.compress(s).length;
        long start = System.nanoTime();
        int count = 0;
        @NotNull byte[] bytes = s.getBytes(ISO_8859_1);
        while (System.nanoTime() < start + 5e9) {
            blackHole = Snappy.compress(bytes);
            count++;
        }
        long avgTime = (System.nanoTime() - start) / count;
        System.out.println("Average time " + avgTime + " ns, saved " + saved + " transfer rate = " + saved * 1000 / avgTime + " MB/s");
    }
}
