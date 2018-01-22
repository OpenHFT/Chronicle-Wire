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
import static org.junit.Assume.assumeTrue;

@RunWith(Parameterized.class)
public final class StoreWritingProcessInHeaderTest {
    private final NativeBytes<Void> bytes;
    private final Wire wire;
    private final WireType wireType;

    public StoreWritingProcessInHeaderTest(final String name, final WireType wireType) {
        bytes = Bytes.allocateElasticDirect();
        wire = wireType.apply(bytes);
        this.wireType = wireType;
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

    @Test
    public void shouldWorkWithMetaDataEntries() throws TimeoutException, EOFException {
        assumeTrue(wireType != WireType.READ_ANY);

        final long position = wire.writeHeaderOfUnknownLength(1, TimeUnit.SECONDS, null, null);
        final int header = wire.bytes().readVolatileInt(position);
        // simulate meta-data indicator in header
        wire.bytes().writeInt(position, header | Wires.META_DATA);
        final int updatedHeader = wire.bytes().readVolatileInt(position);
        assertThat(Wires.isNotComplete(updatedHeader), is(true));
        assertThat(updatedHeader, is(Wires.addMaskedPidToHeader(Wires.NOT_COMPLETE_UNKNOWN_LENGTH | Wires.META_DATA)));
        assertThat(Wires.removeMaskedPidFromHeader(updatedHeader), is(Wires.NOT_COMPLETE_UNKNOWN_LENGTH | Wires.META_DATA));
        assertThat(Wires.extractPidFromHeader(updatedHeader), is(OS.getProcessId()));
        assertThat(Wires.isNotComplete(updatedHeader), is(true));
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
