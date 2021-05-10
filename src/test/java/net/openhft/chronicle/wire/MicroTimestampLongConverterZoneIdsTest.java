package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.ZoneId;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.openhft.chronicle.wire.MicroTimestampLongConverter.INSTANCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

@RunWith(value = Parameterized.class)
public class MicroTimestampLongConverterZoneIdsTest extends WireTestCommon {
    static final long time = INSTANCE.parse("2020/09/18T01:02:03.456789");
    final String zoneId;

    public MicroTimestampLongConverterZoneIdsTest(String zoneId) {
        this.zoneId = zoneId;
    }

    @NotNull
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> combinations() {
        return ZoneId.getAvailableZoneIds().stream()
                .map(z -> new Object[]{z})
                .collect(Collectors.toList());
    }

    @Test
    public void testManyZones() {
        assumeFalse(zoneId.equals("GMT0"));
        MicroTimestampLongConverter mtlc = new MicroTimestampLongConverter(zoneId);
        final String str = mtlc.asString(time);
        assertEquals(zoneId, time, mtlc.parse(str));
    }
}