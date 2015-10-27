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

import net.openhft.chronicle.bytes.NativeBytesStore;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static net.openhft.chronicle.bytes.NativeBytesStore.nativeStoreWithFixedCapacity;

public class BinaryLongReferenceTest {

    @Test
    public void testPingPongUsingAtomics() throws InterruptedException {

        final ExecutorService executorService = Executors.newFixedThreadPool(2);

        AtomicLong inBound = new AtomicLong(1);
        AtomicLong outBound = new AtomicLong(1);

        executorService.submit(() -> {

            try {

                for (; ; ) {
                    long value;
                    do {
                        value = inBound.get();
                    } while (!inBound.compareAndSet(value, 0));

                    for (; ; ) {
                        if (outBound.compareAndSet(0, value))
                            break;
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        });
        CountDownLatch latch = new CountDownLatch(1);
        executorService.submit(() -> {

            long lastValue = 0;

            int i = 0;
            long value = 0;
            for (; value < 1000; ) {
                if (inBound.compareAndSet(0, i)) {


                    do {
                        value = outBound.get();
                    } while (!outBound.compareAndSet(value, 0));


                    if (value != lastValue + 1 && value > 2)
                        throw new AssertionError("");
                    System.out.println("value=" + value);
                    lastValue = value;
                    i++;

                }
            }
            latch.countDown();
        });

        latch.await();

    }


    @Test
    public void testPointPongUsingBinaryLongReference() throws InterruptedException {

        NativeBytesStore<Void> inBoundBytesStore = nativeStoreWithFixedCapacity(8);

        BinaryLongReference inBound = new BinaryLongReference();
        inBound.bytesStore(inBoundBytesStore, 0, 8);
        inBound.setValue(1);

        NativeBytesStore<Void> outBoundBytesStore = nativeStoreWithFixedCapacity(8);

        BinaryLongReference outBound = new BinaryLongReference();
        outBound.bytesStore(outBoundBytesStore, 0, 8);
        outBound.setValue(1);

        final ExecutorService executorService = Executors.newFixedThreadPool(2);

        executorService.submit(() -> {

            try {

                for (; ; ) {
                    long value;
                    do {
                        value = inBound.getVolatileValue();
                    } while (!inBound.compareAndSwapValue(value, 0));

                    for (; ; ) {
                        if (outBound.compareAndSwapValue(0, value))
                            break;
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        });
        CountDownLatch latch = new CountDownLatch(1);
        executorService.submit(() -> {

            long lastValue = 0;

            int i = 0;
            long value = 0;
            for (; value < 1000; ) {
                if (inBound.compareAndSwapValue(0, i)) {

                    do {
                        value = outBound.getVolatileValue();
                    } while (!outBound.compareAndSwapValue(value, 0));


                    if (value != lastValue + 1 && value > 2)
                        throw new AssertionError("");
                    System.out.println("value=" + value);
                    lastValue = value;
                    i++;

                }
            }
            latch.countDown();
        });

        latch.await();

    }
}