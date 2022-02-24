package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.threads.NamedThreadFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class BinaryWireStringInternerTest extends WireTestCommon {
    private static final int DATA_SET_SIZE = 1_000;
    private static final long SEED_WITHOUT_COLLISIONS = 0x982374EADL;

    private final Random random = new Random(SEED_WITHOUT_COLLISIONS);
    private final String[] testData = new String[DATA_SET_SIZE];
    private final String[] internedStrings = new String[DATA_SET_SIZE];
    @SuppressWarnings("rawtypes")
    private final Bytes heapBytes = Bytes.allocateElasticOnHeap(4096);
    private final BinaryWire wire = BinaryWire.binaryOnly(heapBytes);

    private static String message(final int index, final String inputData) {
        return String.format("At index %d for string %s",
                index, inputData);
    }

    private static String makeString(final int length, final Random random) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append((char) ('a' + random.nextInt('z' - 'a')));
        }
        return builder.toString();
    }

    @Before
    public void createTestData() throws Exception {
        for (int i = 0; i < DATA_SET_SIZE; i++) {
            testData[i] = makeString(random.nextInt(250) + 32, random);
        }

        for (int i = 0; i < DATA_SET_SIZE; i++) {
            wire.getFixedBinaryValueOut(true).text(testData[i]);
            internedStrings[i] = wire.read().text();
        }
        wire.clear();
    }

    @Test
    public void shouldInternExistingStringsAlright() throws Exception {
        final List<RuntimeException> capturedExceptions = new CopyOnWriteArrayList<>();

        final ExecutorService executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new NamedThreadFactory("test"));

        for (int i = 0; i < (Jvm.isArm() || Jvm.isCodeCoverage() ? 12 : 200); i++) {
            executorService.submit(new BinaryTextReaderWriter(capturedExceptions::add, () -> BinaryWire.binaryOnly(Bytes.allocateElasticOnHeap(4096))));
        }

        for (int i = 0; i < 50000; i++) {
            wire.clear();
            final int dataPointIndex = random.nextInt(DATA_SET_SIZE);
            wire.getFixedBinaryValueOut(true).text(testData[dataPointIndex]);

            final String inputData = wire.read().text();
            assertEquals(message(i, inputData), internedStrings[dataPointIndex], inputData);
        }

        executorService.shutdown();
        assertTrue("jobs did not complete in time", executorService.awaitTermination(60, TimeUnit.SECONDS));
        assertTrue(capturedExceptions.isEmpty());
    }

    @Test
    public void multipleThreadsUsingBinaryWiresShouldNotCauseProblems() throws Exception {
        final List<RuntimeException> capturedExceptions = new CopyOnWriteArrayList<>();

        final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int i = 0; i < (Jvm.isArm() || Jvm.isCodeCoverage() ? 4 : 200); i++) {
            executorService.submit(new BinaryTextReaderWriter(capturedExceptions::add, () -> BinaryWire.binaryOnly(Bytes.allocateElasticOnHeap(4096))));
        }

        executorService.shutdown();
        assertTrue("jobs did not complete in time",
                executorService.awaitTermination(60, TimeUnit.SECONDS));
        assertTrue(capturedExceptions.isEmpty());
    }

    @Ignore("used to demonstrate errors that can occur when buffers are shared between threads")
    @Test
    public void multipleThreadsSharingBinaryWireShouldCauseProblems() throws Exception {
        final List<RuntimeException> capturedExceptions = new CopyOnWriteArrayList<>();

        final ExecutorService executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new NamedThreadFactory("test"));

        final BinaryWire sharedMutableWire = BinaryWire.binaryOnly(Bytes.allocateElasticOnHeap(4096));
        for (int i = 0; i < 1_000; i++) {
            executorService.submit(new BinaryTextReaderWriter(capturedExceptions::add, () -> sharedMutableWire));
        }

        executorService.shutdown();
        assertTrue("jobs did not complete in time", executorService.awaitTermination(30L, TimeUnit.SECONDS));
        capturedExceptions.stream().filter(e -> e instanceof BufferUnderflowException).forEach(RuntimeException::printStackTrace);
        assertTrue(capturedExceptions.isEmpty());
    }

    private static final class BinaryTextReaderWriter implements Runnable {
        private final Supplier<BinaryWire> binaryWireSupplier;
        private final ThreadLocal<BinaryWire> wire;
        private final Random random = new Random(System.nanoTime());
        private final Consumer<RuntimeException> exceptionConsumer;

        private BinaryTextReaderWriter(final Consumer<RuntimeException> exceptionConsumer,
                                       final Supplier<BinaryWire> binaryWireSupplier) throws IOException {
            this.exceptionConsumer = exceptionConsumer;
            this.binaryWireSupplier = binaryWireSupplier;
            wire = ThreadLocal.withInitial(
                    this.binaryWireSupplier);
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < 2_000; i++) {
                    wire.get().getFixedBinaryValueOut(true).text(makeString(250, random));
                }

                for (int i = 0; i < 2_000; i++) {
                    if (wire.get().read().text() == null) {
                        exceptionConsumer.accept(new IllegalStateException("text was null"));
                    }
                }
            } catch (RuntimeException e) {
                exceptionConsumer.accept(e);
            }
        }
    }
}