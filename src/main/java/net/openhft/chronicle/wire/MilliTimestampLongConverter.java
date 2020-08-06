/*
 * Copyright 2016-2020 Chronicle Software
 *
 * https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

public class MilliTimestampLongConverter implements LongConverter {
    public static final MilliTimestampLongConverter INSTANCE = new MilliTimestampLongConverter();
    final DateTimeFormatter dtf = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
            .toFormatter();

    @Override
    public long parse(CharSequence text) {
        try {
            TemporalAccessor parse = dtf.parse(text);
            long time = parse.getLong(ChronoField.EPOCH_DAY) * 86400_000L;
            if (parse.isSupported(ChronoField.MILLI_OF_DAY))
                time += parse.getLong(ChronoField.MILLI_OF_DAY);
            else if (parse.isSupported(ChronoField.SECOND_OF_DAY))
                time += parse.getLong(ChronoField.SECOND_OF_DAY) * 1_000L;

            return time;
        } catch (DateTimeParseException dtpe) {
            try {
                long number = Long.parseLong(text.toString());
                if (number < 31e9) {
                    System.out.println("In input data, replace " + text + " with a real date.");
                } else {
                    System.out.println("In input data, replace " + text + " with " + asString(number));
                }
                return number;
            } catch (NumberFormatException e) {
                throw dtpe;
            }
        }
    }

    @Override
    public void append(StringBuilder text, long value) {
        LocalDateTime ldt = LocalDateTime.ofEpochSecond(
                value / 1_000,
                (int) (value % 1_000 * 1_000_000),
                ZoneOffset.UTC);
        dtf.formatTo(ldt, text);
    }
}
