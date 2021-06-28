package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeBytes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class WireDumperTest extends WireTestCommon {
    private final NativeBytes<Void> bytes;
    private final Wire wire;
    private final WireType wireType;
    private final Map<WireType, String> expectedContentByType = new HashMap<>();
    private final Map<WireType, String> expectedPartialContent = new HashMap<>();

    public WireDumperTest(final String name, final WireType wireType) {
        bytes = Bytes.allocateElasticDirect();
        wire = wireType.apply(bytes);
        wire.usePadding(true);

        this.wireType = wireType;
        initTestData();
    }

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] parameters() {
        return toParams(WireType.values());
    }

    private static Object[][] toParams(final WireType[] values) {
        return Arrays.stream(values).filter(WireType::isAvailable).
                filter(wt -> wt != WireType.CSV).
                filter(wt -> wt != WireType.READ_ANY).
                filter(wt -> wt != WireType.YAML). // todo
                map(wt -> new Object[]{wt.toString(), wt}).toArray(Object[][]::new);
    }

    @Test
    public void shouldSerialiseContent() {
        wire.writeDocument(17L, ValueOut::int64);
        wire.writeDocument("bark", ValueOut::text);
        wire.writeDocument(3.14D, ValueOut::float64);

        assertEquals(expectedContentByType.get(wireType), WireDumper.of(wire).asString());
    }

    @Test
    public void shouldSerialisePartialContent() {
        wire.writeDocument(17L, ValueOut::int64);
        final DocumentContext context = wire.writingDocument();
        context.wire().getValueOut().text("meow");

        final String actual = WireDumper.of(wire).asString();

        assertEquals(expectedPartialContent.get(wireType), actual);
    }

    @Override
    public void preAfter() {
        bytes.releaseLast();
    }

    private void initTestData() {
        expectedContentByType.put(WireType.TEXT,
                "--- !!data #binary\n" +
                        "00000000             31 37 0a                                 17·          \n" +
                        "# position: 8, header: 1\n" +
                        "--- !!data\n" +
                        "bark\n" +
                        "# position: 20, header: 2\n" +
                        "--- !!data\n" +
                        "3.14\n" +
                        "");

        expectedContentByType.put(WireType.BINARY,
                "--- !!data #binary\n" +
                        "00000000             11                                       ·            \n" +
                        "# position: 8, header: 1\n" +
                        "--- !!data #binary\n" +
                        "bark\n" +
                        "# position: 20, header: 2\n" +
                        "--- !!data #binary\n" +
                        "3.14\n" +
                        "");

        expectedContentByType.put(WireType.BINARY_LIGHT,
                "--- !!data #binary\n" +
                        "00000000             11                                       ·            \n" +
                        "# position: 8, header: 1\n" +
                        "--- !!data #binary\n" +
                        "bark\n" +
                        "# position: 20, header: 2\n" +
                        "--- !!data #binary\n" +
                        "3.14\n" +
                        "");

        expectedContentByType.put(WireType.FIELDLESS_BINARY,
                "--- !!data #binary\n" +
                        "00000000             11                                       ·            \n" +
                        "# position: 8, header: 1\n" +
                        "--- !!data #binary\n" +
                        "bark\n" +
                        "# position: 20, header: 2\n" +
                        "--- !!data #binary\n" +
                        "3.14\n" +
                        "");

        expectedContentByType.put(WireType.COMPRESSED_BINARY,
                "--- !!data #binary\n" +
                        "00000000             11                                       ·            \n" +
                        "# position: 8, header: 1\n" +
                        "--- !!data #binary\n" +
                        "bark\n" +
                        "# position: 20, header: 2\n" +
                        "--- !!data #binary\n" +
                        "3.14\n" +
                        "");

        expectedContentByType.put(WireType.JSON,
                "--- !!data #binary\n" +
                        "00000000             31 37                                    17           \n" +
                        "# position: 8, header: 1\n" +
                        "--- !!data\n" +
                        ",\"bark\"\n" +
                        "# position: 20, header: 2\n" +
                        "--- !!data\n" +
                        ",3.14\n" +
                        "");

        expectedContentByType.put(WireType.RAW,
                "--- !!data #binary\n" +
                        "00000000             11 00 00 00  00 00 00 00                 ···· ····    \n" +
                        "# position: 12, header: 1\n" +
                        "--- !!data #binary\n" +
                        "00000010 04 62 61 72 6b                                   ·bark            \n" +
                        "# position: 24, header: 2\n" +
                        "--- !!data #binary\n" +
                        "00000010                                      1f 85 eb 51              ···Q\n" +
                        "00000020 b8 1e 09 40                                      ···@             \n");

        expectedPartialContent.put(WireType.TEXT,
                "--- !!data #binary\n" +
                        "00000000             31 37 0a                                 17·          \n" +
                        "# position: 8, header: 0 or 1\n" +
                        "--- !!not-ready-data!\n" +
                        "...\n" +
                        "# 5 bytes remaining\n" +
                        "");

        expectedPartialContent.put(WireType.BINARY,
                "--- !!data #binary\n" +
                        "00000000             11                                       ·            \n" +
                        "# position: 8, header: 0 or 1\n" +
                        "--- !!not-ready-data! #binary\n" +
                        "...\n" +
                        "# 5 bytes remaining\n" +
                        "");

        expectedPartialContent.put(WireType.BINARY_LIGHT,
                "--- !!data #binary\n" +
                        "00000000             11                                       ·            \n" +
                        "# position: 8, header: 0 or 1\n" +
                        "--- !!not-ready-data! #binary\n" +
                        "...\n" +
                        "# 5 bytes remaining\n" +
                        "");

        expectedPartialContent.put(WireType.FIELDLESS_BINARY,
                "--- !!data #binary\n" +
                        "00000000             11                                       ·            \n" +
                        "# position: 8, header: 0 or 1\n" +
                        "--- !!not-ready-data! #binary\n" +
                        "...\n" +
                        "# 5 bytes remaining\n" +
                        "");

        expectedPartialContent.put(WireType.COMPRESSED_BINARY,
                "--- !!data #binary\n" +
                        "00000000             11                                       ·            \n" +
                        "# position: 8, header: 0 or 1\n" +
                        "--- !!not-ready-data! #binary\n" +
                        "...\n" +
                        "# 5 bytes remaining\n" +
                        "");

        expectedPartialContent.put(WireType.JSON,
                "--- !!data #binary\n" +
                        "00000000             31 37                                    17           \n" +
                        "# position: 8, header: 0 or 1\n" +
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
}