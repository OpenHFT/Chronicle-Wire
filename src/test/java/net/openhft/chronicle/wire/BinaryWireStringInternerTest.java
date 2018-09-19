package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public final class BinaryWireStringInternerTest {
    private static final int DATA_SET_SIZE = 1_000;
    private static final long SEED_WITHOUT_COLLISIONS = 0x982374EADL;

    private final Random random = new Random(SEED_WITHOUT_COLLISIONS);
    private final String[] testData = new String[DATA_SET_SIZE];
    private final String[] internedStrings = new String[DATA_SET_SIZE];
    private final Bytes heapBytes = Bytes.elasticHeapByteBuffer(4096);
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
    public void shouldInternExistingStrings() {
        for (int i = 0; i < 5; i++) {
            wire.clear();
            final int dataPointIndex = random.nextInt(DATA_SET_SIZE);
            wire.getFixedBinaryValueOut(true).text(testData[dataPointIndex]);

            final String inputData = wire.read().text();
            assertThat(message(i, inputData), inputData, is(internedStrings[dataPointIndex]));
            assertThat(message(i, inputData), System.identityHashCode(inputData),
                    is(System.identityHashCode(internedStrings[dataPointIndex])));
            assertThat(message(i, inputData), inputData, sameInstance(internedStrings[dataPointIndex]));
        }
    }

    @Test
    public void multipleThreadsUsingBinaryWiresShouldNotCauseProblems() throws Exception {
        final List<RuntimeException> capturedExceptions = new CopyOnWriteArrayList<>();

        final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int i = 0; i < (Jvm.isArm() ? 12 : 200); i++) {
            executorService.submit(new BinaryTextReaderWriter(capturedExceptions::add, () -> BinaryWire.binaryOnly(Bytes.elasticHeapByteBuffer(4096))));
        }

        executorService.shutdown();
        assertTrue("jobs did not complete in time", executorService.awaitTermination(5L, TimeUnit.SECONDS));
        assertThat(capturedExceptions.isEmpty(), is(true));
    }

    @Ignore("used to demonstrate errors that can occur when buffers are shared between threads")
    @Test
    public void multipleThreadsSharingBinaryWireShouldCauseProblems() throws Exception {
        final List<RuntimeException> capturedExceptions = new CopyOnWriteArrayList<>();

        final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        final BinaryWire sharedMutableWire = BinaryWire.binaryOnly(Bytes.elasticHeapByteBuffer(4096));
        for (int i = 0; i < 1_000; i++) {
            executorService.submit(new BinaryTextReaderWriter(capturedExceptions::add, () -> sharedMutableWire));
        }

        executorService.shutdown();
        assertTrue("jobs did not complete in time", executorService.awaitTermination(15L, TimeUnit.SECONDS));
        capturedExceptions.stream().filter(e -> e instanceof BufferUnderflowException).forEach(RuntimeException::printStackTrace);
        assertThat(capturedExceptions.isEmpty(), is(true));
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