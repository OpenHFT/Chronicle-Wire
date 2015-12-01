/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.wire;

import org.xerial.snappy.Snappy;

import java.io.IOException;

/**
 * Created by peter on 01/12/15.
 */
public class SnappyComparisonMain {
    static volatile byte[] blackHole = null;

    public static void main(String... args) throws IOException {
        test(100);
        for (int i = 100; i <= 1 << 20; i *= 2) {
            test(i);
        }
    }

    private static void test(int length) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length)
            sb.append(Integer.toBinaryString(sb.length()));
        String s = sb.toString();
        System.out.println(s.length() + " compressed to " + Snappy.compress(s).length);
        int saved = s.length() - Snappy.compress(s).length;
        long start = System.nanoTime();
        int count = 0;
        byte[] bytes = s.getBytes();
        while(System.nanoTime() < start + 5e9) {
            blackHole = Snappy.compress(bytes);
            count++;
        }
        long avgTime = (System.nanoTime() - start) / count;
        System.out.println("Average time "+avgTime+" ns, saved "+saved+" transfer rate = "+saved*1000/avgTime+" MB/s");
    }
}
