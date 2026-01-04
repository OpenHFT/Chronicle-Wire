/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.wire.JSONWire;
import net.openhft.chronicle.wire.JsonUtil;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.jupiter.api.DisplayName;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

class Issue327Test extends WireTestCommon {

    private boolean useTypes;

    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{true},
                new Object[]{false}
        );
    }

    public void initIssue327Test(boolean useTypes) {
        this.useTypes = useTypes;
    }

    @DisplayName("Local time serialises with optional type metadata")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} local time JSON output string")
    void localTime(boolean useTypes) {
        initIssue327Test(useTypes);
        String actual = toJson(() -> LocalTime.of(17, 01));
        assertEquals(useTypes ? "{\"@Time\":\"17:01\"}" : "\"17:01\"", actual,
                "Local time JSON should match expected output (useTypes=" + useTypes + ")");
    }

    @DisplayName("Local date time serialises with optional type metadata")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} local date-time JSON output string")
    void localDateTime(boolean useTypes) {
        initIssue327Test(useTypes);
        String actual = toJson(() -> LocalDateTime.of(1969, 7, 20, 20, 17, 01));
        assertEquals(useTypes ? "{\"@DateTime\":\"1969-07-20T20:17:01\"}" : "\"1969-07-20T20:17:01\"", actual,
                "Local date-time JSON should match expected output (useTypes=" + useTypes + ")");
    }

    @DisplayName("Zoned date time serialises with optional type metadata")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} zoned date-time JSON output string")
    void zonedDateTime(boolean useTypes) {
        initIssue327Test(useTypes);
        String actual = toJson(() -> ZonedDateTime.of(LocalDateTime.of(1969, 7, 20, 20, 17, 01), ZoneId.of("UTC")));
        assertEquals(useTypes ? "{\"@ZonedDateTime\":\"1969-07-20T20:17:01Z[UTC]\"}" : "\"1969-07-20T20:17:01Z[UTC]\"", actual,
                "Zoned date-time JSON should match expected output (useTypes=" + useTypes + ")");
    }

    @DisplayName("UUID serialises with optional type metadata")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} UUID JSON output string")
    void uIID(boolean useTypes) {
        initIssue327Test(useTypes);
        String actual = toJson(() -> UUID.fromString("b2f78c98-b07d-42ab-86d5-4b0d48550761"));
        assertEquals(useTypes ? "{\"@UUID\":\"b2f78c98-b07d-42ab-86d5-4b0d48550761\"}" : "\"b2f78c98-b07d-42ab-86d5-4b0d48550761\"", actual,
                "UUID JSON should match expected output (useTypes=" + useTypes + ")");
    }

    @DisplayName("SQL date serialises with optional type metadata")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} SQL date JSON output string")
    void date(boolean useTypes) {
        initIssue327Test(useTypes);
        String actual = toJson(() -> java.sql.Date.valueOf("1969-07-20"));
        assertEquals(useTypes ? "{\"@java.sql.Date\":\"1969-07-20T00:00:00.000 GMT\"}" : "\"1969-07-20T00:00:00.000 GMT\"", actual,
                "SQL date JSON should match expected output (useTypes=" + useTypes + ")");
    }

    @DisplayName("Byte array serialises with optional type metadata")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} byte array JSON output string")
    void byteArray(boolean useTypes) {
        initIssue327Test(useTypes);
        //test(() -> "Buzz".getBytes(StandardCharsets.UTF_8), "{\"@byte[]\":{\"@!binary\":\"QnV6eg==\"}}", "QnV6eg==");
        // not sure what the expected typed output should be
        String actual = toJson(() -> "Buzz".getBytes(StandardCharsets.UTF_8));
        assertEquals(useTypes ? "{\"@byte[]\":{\"@!binary\":\"QnV6eg==\"}}" : "\"QnV6eg==\"", actual,
                "Byte array JSON should match expected output (useTypes=" + useTypes + ")");
    }

    @DisplayName("Integer array serialises with optional type metadata")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} int array JSON output string")
    void intArray(boolean useTypes) {
        initIssue327Test(useTypes);
        String actual = toJson(() -> IntStream.range(0, 4).toArray());
        assertEquals(useTypes ? "{\"@int[]\":[0,1,2,3 ]}" : "[0,1,2,3 ]", actual,
                "Int array JSON should match expected output (useTypes=" + useTypes + ")");
    }

    @DisplayName("File serialises with optional type metadata")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} file JSON output string")
    void file(boolean useTypes) {
        initIssue327Test(useTypes);
        String actual = toJson(() -> new File("info.txt"));
        assertEquals(useTypes ? "{\"@java.io.File\":\"info.txt\"}" : "\"info.txt\"", actual,
                "File JSON should match expected output (useTypes=" + useTypes + ")");
    }

    @DisplayName("BigDecimal serialises with optional type metadata")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "useTypes={0} BigDecimal JSON output string")
    void bigDecimal(boolean useTypes) {
        initIssue327Test(useTypes);
        String actual = toJson(() -> BigDecimal.TEN);
        assertEquals(useTypes ? "{\"@java.math.BigDecimal\":\"10\"}" : "\"10\"", actual,
                "BigDecimal JSON should match expected output (useTypes=" + useTypes + ")");
    }

    private <T> String toJson(final Supplier<T> constructor) {
        final Wire wire = new JSONWire().useTypes(useTypes);
        final T target = constructor.get();

        wire.getValueOut()
                .object(target);
        final String actual = wire.toString();
        JsonUtil.assertBalancedBrackets(actual);
        return actual;
    }
}
