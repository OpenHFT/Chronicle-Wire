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

package net.openhft.chronicle.wire.domestic.streaming;

import net.openhft.chronicle.wire.MarshallableIn;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.domestic.stream.Streams;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static net.openhft.chronicle.wire.domestic.streaming.CreateUtil.createThenValueOuts;
import static org.junit.jupiter.api.Assertions.*;

class StreamsTest {


    @Test
    void streamTextMessage() {
        MarshallableIn tailer = createThenValueOuts(a -> a.writeString("Hello"));

        Stream<String> stream = Streams.of(tailer, (w, index) -> w.getValueIn().text());
        assertFalse(stream.isParallel());
        List<String> actualContent = stream.collect(toList());
        assertEquals(Collections.singletonList("Hello"), actualContent);
    }

    @Test
    void iteratorEmpty() {
        Wire tailer = CreateUtil.create();

        Iterator<String> iterator = Streams.iterator(tailer, (wire, index) -> "A");
        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasNext());
    }

    @Test
    void iteratorTextMessage() {
        MarshallableIn tailer = createThenValueOuts(a -> a.writeString("Hello"));
        Iterator<String> iterator = Streams.iterator(tailer, (wire, index) -> wire.getValueIn().text());
        assertTrue(iterator.hasNext());
        // Make sure another call does not change the state
        assertTrue(iterator.hasNext());
        assertEquals("Hello", iterator.next());
        assertFalse(iterator.hasNext());
    }

}