/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.wire.JSONWire;
import net.openhft.chronicle.wire.JsonUtil;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class Issue327Test extends WireTestCommon {

    private boolean useTypes;

    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{true},
                new Object[]{false}
        );
    }

    @ParameterizedTest
    @MethodSource("wireTypes")
    void localTime(boolean useTypes) {
        this.useTypes = useTypes;
        test(() -> LocalTime.of(17, 01), "{\"@Time\":\"17:01\"}", "\"17:01\"");
    }

    @ParameterizedTest
    @MethodSource("wireTypes")
    void localDateTime(boolean useTypes) {
        this.useTypes = useTypes;
        test(() -> LocalDateTime.of(1969, 7, 20, 20, 17, 01), "{\"@DateTime\":\"1969-07-20T20:17:01\"}", "\"1969-07-20T20:17:01\"");
    }

    @ParameterizedTest
    @MethodSource("wireTypes")
    void zonedDateTime(boolean useTypes) {
        this.useTypes = useTypes;
        test(() -> ZonedDateTime.of(LocalDateTime.of(1969, 7, 20, 20, 17, 01), ZoneId.of("UTC")), "{\"@ZonedDateTime\":\"1969-07-20T20:17:01Z[UTC]\"}", "\"1969-07-20T20:17:01Z[UTC]\"");
    }

    @ParameterizedTest
    @MethodSource("wireTypes")
    void uIID(boolean useTypes) {
        this.useTypes = useTypes;
        test(() -> UUID.fromString("b2f78c98-b07d-42ab-86d5-4b0d48550761"), "{\"@UUID\":\"b2f78c98-b07d-42ab-86d5-4b0d48550761\"}", "\"b2f78c98-b07d-42ab-86d5-4b0d48550761\"");
    }

    @ParameterizedTest
    @MethodSource("wireTypes")
    void date(boolean useTypes) {
        this.useTypes = useTypes;
        test(() -> java.sql.Date.valueOf("1969-07-20"), "{\"@java.sql.Date\":\"1969-07-20T00:00:00.000 GMT\"}", "\"1969-07-20T00:00:00.000 GMT\"");
    }

    @ParameterizedTest
    @MethodSource("wireTypes")
    void byteArray(boolean useTypes) {
        this.useTypes = useTypes;
        //test(() -> "Buzz".getBytes(StandardCharsets.UTF_8), "{\"@byte[]\":{\"@!binary\":\"QnV6eg==\"}}", "QnV6eg==");
        // not sure what the expected typed output should be
        test(() -> "Buzz".getBytes(StandardCharsets.UTF_8), "{\"@byte[]\":{\"@!binary\":\"QnV6eg==\"}}", "\"QnV6eg==\"");
    }

    @ParameterizedTest
    @MethodSource("wireTypes")
    void intArray(boolean useTypes) {
        this.useTypes = useTypes;
        test(() -> IntStream.range(0, 4).toArray(), "{\"@int[]\":[0,1,2,3 ]}", "[0,1,2,3 ]");
    }

    @ParameterizedTest
    @MethodSource("wireTypes")
    void file(boolean useTypes) {
        this.useTypes = useTypes;
        test(() -> new File("info.txt"), "{\"@java.io.File\":\"info.txt\"}", "\"info.txt\"");
    }

    @ParameterizedTest
    @MethodSource("wireTypes")
    void bigDecimal(boolean useTypes) {
        this.useTypes = useTypes;
        test(() -> BigDecimal.TEN, "{\"@java.math.BigDecimal\":\"10\"}", "\"10\"");
    }

    private <T> void test(final Supplier<T> constructor,
                          final String expectedTyped,
                          final String expected) {
        Wire wire = new JSONWire().useTypes(useTypes);
        final T target = constructor.get();

        wire.getValueOut()
                .object(target);
        final String actual = wire.toString();
        System.out.println("actual = " + actual);
        JsonUtil.assertBalancedBrackets(actual);
        if (useTypes)
            assertEquals(expectedTyped, actual);
        else
            assertEquals(expected, actual);
    }
}
