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

// Components are not required to be Closeable, but if they are they should be closed when finished
public class TestImpl extends SimpleCloseable implements TestIn {
    private final TestOut out;
    private long time, prevEventTime;

    public TestImpl(TestOut out) {
        this.out = out;
    }

    @Override
    public void time(@LongConversion(NanoTimestampLongConverter.class) long time) {
        if (time < this.time)
            out.error("Time cannot be turned backwards");
        this.time = time;
    }

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

    @Override
    public void testAbstractMarshallableCfgEvent(TestAbstractMarshallableCfgEvent dto) {
        out.testAbstractMarshallableCfgEvent(dto);
    }
}
