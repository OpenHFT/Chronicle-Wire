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

package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.AbstractCloseable;
import net.openhft.chronicle.core.io.Closeable;
import org.junit.Assert;
import org.junit.Test;

// see https://github.com/OpenHFT/Chronicle-Wire/issues/225
public class WireResetTest extends WireTestCommon {
    @Test
    public void test() {
        Event event = new Event();
        Assert.assertFalse(event.isClosed());

        event.reset();
        Assert.assertFalse(event.isClosed());
    }

    @Test
    public void testEventAbstractCloseable() {
        try (EventAbstractCloseable event = new EventAbstractCloseable()) {
            Assert.assertFalse(event.isClosed());

            event.reset();
            Assert.assertFalse(event.isClosed());
        }
    }

    public static class Event extends SelfDescribingMarshallable implements Closeable {
        private boolean isClosed;

        //other fields

        @Override
        public void close() {
            isClosed = true;
        }

        @Override
        public boolean isClosed() {
            return isClosed;
        }
    }

    public static class EventAbstractCloseable extends AbstractCloseable implements Marshallable {
        @Override
        protected void performClose() {

        }
    }
}