/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
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

package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.core.io.SimpleCloseable;
import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.NanoTimestampLongConverter;

import static net.openhft.chronicle.core.time.SystemTimeProvider.CLOCK;

/**
 * The TestImpl class extends SimpleCloseable and implements the TestIn interface.
 * It is designed to handle various events, ensuring time consistency and processing of event data.
 */
public class TestImpl extends SimpleCloseable implements TestIn {

    // Reference to an instance of TestOut for output operations.
    private final TestOut out;

    // Stores the latest time provided, ensuring chronological order of events.
    private long time, prevEventTime;

    /**
     * Constructor that initializes the TestImpl instance with a given TestOut instance.
     *
     * @param out Instance of TestOut to send outputs or responses to.
     */
    public TestImpl(TestOut out) {
        this.out = out;
    }

    /**
     * Processes a time value with nanosecond precision, ensuring it is not set to a past value.
     *
     * @param time The time value to be processed, represented in nanoseconds.
     */
    @Override
    public void time(@LongConversion(NanoTimestampLongConverter.class) long time) {
        if (time < this.time)
            out.error("Time cannot be turned backwards");
        this.time = time;
    }

    /**
     * Processes a TestEvent, checks for chronological order, and sets processed and current times.
     * If the event time is older than the previous, an error is outputted.
     *
     * @param dto The TestEvent data transfer object containing event details.
     */
    @Override
    public void testEvent(TestEvent dto) {
        if (dto.eventTime < prevEventTime)
            out.error("The eventTime was older than a previous message");
        prevEventTime = dto.eventTime;
        if (time != 0)
            dto.processedTime = time;
        dto.currentTime = CLOCK.currentTimeNanos();
        out.testEvent(dto);
    }

    /**
     * Processes a TestAbstractMarshallableCfgEvent and forwards it through the TestOut interface.
     *
     * @param dto The TestAbstractMarshallableCfgEvent data transfer object to be processed.
     */
    @Override
    public void testAbstractMarshallableCfgEvent(TestAbstractMarshallableCfgEvent dto) {
        out.testAbstractMarshallableCfgEvent(dto);
    }
}
