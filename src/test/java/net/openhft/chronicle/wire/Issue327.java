package net.openhft.chronicle.wire;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static net.openhft.chronicle.wire.JsonUtil.assertBalancedBrackets;
import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class Issue327 {

    private final boolean useTypes;

    @Parameterized.Parameters(name = "useTypes={0}")
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{true},
                new Object[]{false}
        );
    }

    public Issue327(boolean useTypes) {
        this.useTypes = useTypes;
    }

    @Test
    public void localTime() {
        test(() -> LocalTime.of(17, 01), "{\"@Time\":\"17:01\"}", "\"17:01\"");
    }

    @Test
    public void localDateTime() {
        test(() -> LocalDateTime.of(1969, 7, 20, 20, 17, 01), "{\"@DateTime\":\"1969-07-20T20:17:01\"}", "\"1969-07-20T20:17:01\"");
    }

    @Test
    public void zonedDateTime() {
        test(() -> ZonedDateTime.of(LocalDateTime.of(1969, 7, 20, 20, 17, 01), ZoneId.of("UTC")), "{\"@ZonedDateTime\":\"1969-07-20T20:17:01Z[UTC]\"}", "\"1969-07-20T20:17:01Z[UTC]\"");
    }

    @Test
    public void uIID() {
        test(() -> UUID.fromString("b2f78c98-b07d-42ab-86d5-4b0d48550761"), "{\"@UUID\":\"b2f78c98-b07d-42ab-86d5-4b0d48550761\"}", "\"b2f78c98-b07d-42ab-86d5-4b0d48550761\"");
    }

    @Test
    public void date() {
        test(() -> Date.valueOf("1969-07-20"), "{\"@java.sql.Date\":\"1969-07-20\"}", "\"1969-07-20\"");
    }

    @Test
    public void byteArray() {
        //test(() -> "Buzz".getBytes(StandardCharsets.UTF_8), "{\"@byte[]\":{\"@!binary\":\"QnV6eg==\"}}", "QnV6eg==");
        // not sure what the expected typed output should be
        test(() -> "Buzz".getBytes(StandardCharsets.UTF_8), "{\"@byte[]\":{\"@!binary\":\"QnV6eg==\"}}", "\"QnV6eg==\"");
    }

    @Test
    public void intArray() {
        test(() -> IntStream.range(0, 4).toArray(), "{\"@int[]\":[ 0,1,2,3 ]}", "[ 0,1,2,3 ]");
    }

    @Test
    public void file() {
        test(() -> new File("~/info.txt"), "{\"@java.io.File\":\"~/info.txt\"}", "\"~/info.txt\"");
    }

    @Test
    public void bigDecimal() {
        test(() -> BigDecimal.TEN, "{\"@java.math.BigDecimal\":\"10\"}", "\"10\"");
    }

    private <T> void test(final Supplier<T> constructor,
                          final String expectedTyped,
                          final String expected) {
        final Wire wire = new JSONWire().useTypes(useTypes);
        final T target = constructor.get();

        wire.getValueOut()
                .object(target);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        assertBalancedBrackets(actual);
        if (useTypes)
            assertEquals(expectedTyped, actual);
        else
            assertEquals(expected, actual);
    }


}
