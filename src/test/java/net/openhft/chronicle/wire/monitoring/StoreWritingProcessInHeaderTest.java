package net.openhft.chronicle.wire.monitoring;

import net.openhft.affinity.Affinity;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeBytes;
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

    @BeforeClass
    public static void enableFeature() {
        Wires.encodeTidInHeader(true);
    }

    @AfterClass
    public static void disableFeature() {
        Wires.encodeTidInHeader(false);
    }

    private static Object[][] toParams(final WireType[] values) {
        return Arrays.stream(values).filter(WireType::isAvailable).
                filter(wt -> wt != WireType.CSV).
                map(wt -> new Object[]{wt.toString(), wt}).toArray(Object[][]::new);
    }

    @Test
    public void shouldEncodePid() {
        final int tid = Affinity.getThreadId();
        final int headerWithTid = Wires.addMaskedTidToHeader(Wires.NOT_COMPLETE_UNKNOWN_LENGTH);

        assertThat(Wires.isNotComplete(headerWithTid), is(true));
        assertThat(headerWithTid, is(not(Wires.NOT_COMPLETE_UNKNOWN_LENGTH)));
        assertThat(Wires.extractTidFromHeader(headerWithTid), is(tid));
        assertThat(Wires.removeMaskedTidFromHeader(headerWithTid), is(Wires.NOT_COMPLETE_UNKNOWN_LENGTH));
    }

    @Test
    public void shouldStoreWritingProcessIdInHeader() throws TimeoutException, EOFException {
        final long position = wire.writeHeaderOfUnknownLength(1, TimeUnit.SECONDS, null, null);
        final int header = wire.bytes().readVolatileInt(position);
        assertThat(Wires.isNotComplete(header), is(true));
        assertThat(header, is(Wires.addMaskedTidToHeader(Wires.NOT_COMPLETE_UNKNOWN_LENGTH)));
        assertThat(Wires.removeMaskedTidFromHeader(header), is(Wires.NOT_COMPLETE_UNKNOWN_LENGTH));
        assertThat(Wires.extractTidFromHeader(header), is(Affinity.getThreadId()));
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
        assertThat(updatedHeader, is(Wires.addMaskedTidToHeader(Wires.NOT_COMPLETE_UNKNOWN_LENGTH | Wires.META_DATA)));
        assertThat(Wires.removeMaskedTidFromHeader(updatedHeader), is(Wires.NOT_COMPLETE_UNKNOWN_LENGTH | Wires.META_DATA));
        assertThat(Wires.extractTidFromHeader(updatedHeader), is(Affinity.getThreadId()));
        assertThat(Wires.isNotComplete(updatedHeader), is(true));
    }

    @After
    public void tearDown() {
        bytes.release();
    }
}
