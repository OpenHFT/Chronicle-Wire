package net.openhft.chronicle.wire.monitoring;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import net.openhft.chronicle.wire.Wires;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
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

        assertThat(Wires.isNotComplete(headerWithPid), is(true));
        assertThat(headerWithPid, is(not(Wires.NOT_COMPLETE_UNKNOWN_LENGTH)));
        assertThat(Wires.extractPidFromHeader(headerWithPid), is(pid));
        assertThat(Wires.removeMaskedPidFromHeader(headerWithPid), is(Wires.NOT_COMPLETE_UNKNOWN_LENGTH));
    }

    @Test
    public void shouldStoreWritingProcessIdInHeader() throws TimeoutException, EOFException {
        final long position = wire.writeHeaderOfUnknownLength(1, TimeUnit.SECONDS, null, null);

        final int header = wire.bytes().readVolatileInt(position);
        assertThat(Wires.isNotComplete(header), is(true));
        assertThat(header, is(Wires.addMaskedPidToHeader(Wires.NOT_COMPLETE_UNKNOWN_LENGTH)));
        assertThat(Wires.removeMaskedPidFromHeader(header), is(Wires.NOT_COMPLETE_UNKNOWN_LENGTH));
        assertThat(Wires.extractPidFromHeader(header), is(OS.getProcessId()));
    }

    @After
    public void tearDown() {
        bytes.release();
    }

    @BeforeClass
    public static void enableFeature() {
        System.setProperty("wire.encodePidInHeader", Boolean.TRUE.toString());
    }

    @AfterClass
    public static void disableFeature() {
        System.setProperty("wire.encodePidInHeader", Boolean.FALSE.toString());
    }

    private static Object[][] toParams(final WireType[] values) {
        return Arrays.stream(values).filter(WireType::isAvailable).
                filter(wt -> wt != WireType.CSV).
                map(wt -> new Object[] {wt.toString(), wt}).toArray(Object[][]::new);
    }
}
