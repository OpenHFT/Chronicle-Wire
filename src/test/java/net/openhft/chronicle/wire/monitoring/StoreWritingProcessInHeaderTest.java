package net.openhft.chronicle.wire.monitoring;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import net.openhft.chronicle.wire.Wires;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.EOFException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public final class StoreWritingProcessInHeaderTest {

    private final NativeBytes<Void> bytes;
    private final Wire wire;

    public StoreWritingProcessInHeaderTest(final String name, final WireType wireType) {
        bytes = Bytes.allocateElasticDirect();
        wire = wireType.apply(bytes);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] parameters() {
        return toParams(WireType.values());
    }

    @Test
    public void shouldEncodePid() {
        final int pid = OS.getProcessId();
        final int headerWithPid = Wires.addMaskedPidToHeader(Wires.NOT_COMPLETE_UNKNOWN_LENGTH);

        assertThat(headerWithPid, is(not(Wires.NOT_COMPLETE_UNKNOWN_LENGTH)));
        assertThat(Wires.extractPidFromHeader(headerWithPid), is(pid));
        assertThat(Wires.removeMaskedPidFromHeader(headerWithPid), is(Wires.NOT_COMPLETE_UNKNOWN_LENGTH));
    }

    @Test
    public void shouldStoreWritingProcessIdInHeader() throws TimeoutException, EOFException {
        final long position = wire.writeHeaderOfUnknownLength(1, TimeUnit.SECONDS, null, null);

        assertThat(wire.bytes().readVolatileInt(position), is(Wires.NOT_COMPLETE_UNKNOWN_LENGTH));
    }

    @After
    public void tearDown() {
        bytes.release();
    }

    private static Object[][] toParams(final WireType[] values) {
        return Arrays.stream(values).filter(WireType::isAvailable).
                filter(wt -> wt != WireType.CSV).
                map(wt -> new Object[] {wt.toString(), wt}).toArray(Object[][]::new);
    }
}
