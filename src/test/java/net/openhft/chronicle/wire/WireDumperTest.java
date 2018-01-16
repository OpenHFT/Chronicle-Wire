package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.wire.monitoring.StoreWritingProcessInHeaderTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class WireDumperTest {
    private final NativeBytes<Void> bytes;
    private final Wire wire;
    private final WireType wireType;
    private final Map<WireType, String> expectedContentByType = new HashMap<>();
    private final Map<WireType, String> expectedPartialContent = new HashMap<>();

    public WireDumperTest(final String name, final WireType wireType) {
        bytes = Bytes.allocateElasticDirect();
        wire = wireType.apply(bytes);
        this.wireType = wireType;
        initTestData();
    }

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] parameters() {
        return toParams(WireType.values());
    }

    @Test
    public void shouldSerialiseContent() {
        wire.writeDocument(17L, ValueOut::int64);
        wire.writeDocument("bark", ValueOut::text);
        wire.writeDocument(3.14D, ValueOut::float64);

        assertThat(WireDumper.of(wire).asString(), is(expectedContentByType.get(wireType)));
    }

    @Test
    public void shouldSerialisePartialContent() {
        wire.writeDocument(17L, ValueOut::int64);
        final DocumentContext context = wire.writingDocument();
        context.wire().getValueOut().text("meow");

        final String actual = WireDumper.of(wire).asString();

        assertThat(actual, is(expectedPartialContent.get(wireType)));
    }

    @BeforeClass
    public static void beforeClass() {
        StoreWritingProcessInHeaderTest.enableFeature();
    }

    @AfterClass
    public static void afterClass() {
        StoreWritingProcessInHeaderTest.disableFeature();
    }

    @After
    public void tearDown() {
        bytes.release();
    }

    private void initTestData() {
        expectedContentByType.put(WireType.TEXT,
                "--- !!data #binary\n" +
                        "00000000             31 37 0a                                 17·          \n" +
                        "# position: 7, header: 1\n" +
                        "--- !!data\n" +
                        "bark\n" +
                        "# position: 16, header: 2\n" +
                        "--- !!data\n" +
                        "3.14\n" +
                        "");

        expectedContentByType.put(WireType.BINARY,
                "--- !!data #binary\n" +
                        "00000000             11                                       ·            \n" +
                        "# position: 5, header: 1\n" +
                        "--- !!data #binary\n" +
                        "bark\n" +
                        "# position: 14, header: 2\n" +
                        "--- !!data #binary\n" +
                        "3.14\n" +
                        "");

        expectedContentByType.put(WireType.BINARY_LIGHT,
                "--- !!data #binary\n" +
                        "00000000             11                                       ·            \n" +
                        "# position: 5, header: 1\n" +
                        "--- !!data #binary\n" +
                        "bark\n" +
                        "# position: 14, header: 2\n" +
                        "--- !!data #binary\n" +
                        "3.14\n" +
                        "");

        expectedContentByType.put(WireType.FIELDLESS_BINARY,
                "--- !!data #binary\n" +
                        "00000000             11                                       ·            \n" +
                        "# position: 5, header: 1\n" +
                        "--- !!data #binary\n" +
                        "bark\n" +
                        "# position: 14, header: 2\n" +
                        "--- !!data #binary\n" +
                        "3.14\n" +
                        "");

        expectedContentByType.put(WireType.COMPRESSED_BINARY,
                "--- !!data #binary\n" +
                        "00000000             11                                       ·            \n" +
                        "# position: 5, header: 1\n" +
                        "--- !!data #binary\n" +
                        "bark\n" +
                        "# position: 14, header: 2\n" +
                        "--- !!data #binary\n" +
                        "3.14\n" +
                        "");

        expectedContentByType.put(WireType.JSON,
                "--- !!data #binary\n" +
                        "00000000             31 37                                    17           \n" +
                        "# position: 6, header: 1\n" +
                        "--- !!data\n" +
                        ",\"bark\"\n" +
                        "# position: 17, header: 2\n" +
                        "--- !!data\n" +
                        ",3.14\n" +
                        "");

        expectedContentByType.put(WireType.RAW,
                "--- !!data #binary\n" +
                        "00000000             11 00 00 00  00 00 00 00                 ···· ····    \n" +
                        "# position: 12, header: 1\n" +
                        "--- !!data #binary\n" +
                        "00000010 04 62 61 72 6b                                   ·bark            \n" +
                        "# position: 21, header: 2\n" +
                        "--- !!data #binary\n" +
                        "00000010                             1f 85 eb 51 b8 1e 09           ···Q···\n" +
                        "00000020 40                                               @                \n" +
                        "");

        expectedPartialContent.put(WireType.TEXT,
                "--- !!data #binary\n" +
                        "00000000             31 37 0a                                 17·          \n" +
                        "# position: 7, header: 0 or 1\n" +
                        "--- !!not-ready-data!\n" +
                        "...\n" +
                        "# 5 bytes remaining\n" +
                        "");


        expectedPartialContent.put(WireType.BINARY,
                "--- !!data #binary\n" +
                        "00000000             11                                       ·            \n" +
                        "# position: 5, header: 0 or 1\n" +
                        "--- !!not-ready-data! #binary\n" +
                        "...\n" +
                        "# 5 bytes remaining\n" +
                        "");


        expectedPartialContent.put(WireType.BINARY_LIGHT,
                "--- !!data #binary\n" +
                        "00000000             11                                       ·            \n" +
                        "# position: 5, header: 0 or 1\n" +
                        "--- !!not-ready-data! #binary\n" +
                        "...\n" +
                        "# 5 bytes remaining\n" +
                        "");


        expectedPartialContent.put(WireType.FIELDLESS_BINARY,
                "--- !!data #binary\n" +
                        "00000000             11                                       ·            \n" +
                        "# position: 5, header: 0 or 1\n" +
                        "--- !!not-ready-data! #binary\n" +
                        "...\n" +
                        "# 5 bytes remaining\n" +
                        "");


        expectedPartialContent.put(WireType.COMPRESSED_BINARY,
                "--- !!data #binary\n" +
                        "00000000             11                                       ·            \n" +
                        "# position: 5, header: 0 or 1\n" +
                        "--- !!not-ready-data! #binary\n" +
                        "...\n" +
                        "# 5 bytes remaining\n" +
                        "");


        expectedPartialContent.put(WireType.JSON,
                "--- !!data #binary\n" +
                        "00000000             31 37                                    17           \n" +
                        "# position: 6, header: 0 or 1\n" +
                        "--- !!not-ready-data!\n" +
                        "...\n" +
                        "# 7 bytes remaining\n" +
                        "");


        expectedPartialContent.put(WireType.RAW,
                "--- !!data #binary\n" +
                        "00000000             11 00 00 00  00 00 00 00                 ···· ····    \n" +
                        "# position: 12, header: 0 or 1\n" +
                        "--- !!not-ready-data! #binary\n" +
                        "...\n" +
                        "# 5 bytes remaining\n" +
                        "");

    }

    private static Object[][] toParams(final WireType[] values) {
        return Arrays.stream(values).filter(WireType::isAvailable).
                filter(wt -> wt != WireType.CSV).
                filter(wt -> wt != WireType.READ_ANY).
                map(wt -> new Object[] {wt.toString(), wt}).toArray(Object[][]::new);
    }
}