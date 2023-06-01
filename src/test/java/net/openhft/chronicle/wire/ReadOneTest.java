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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReadOneTest extends WireTestCommon {

    static class MyDto extends SelfDescribingMarshallable {
        String data;
    }

    interface MyDtoListener {
        void myDto(MyDto dto);
    }

    static class SnapshotDTO extends SelfDescribingMarshallable {
        String data;

        public String data() {
            return data;
        }

        public SnapshotDTO data(String data) {
            this.data = data;
            return this;
        }
    }

    interface SnapshotListener {
        void snapshot(SnapshotDTO dto);
    }

    @Test
    public void test() throws InterruptedException {
        doTest(false);
    }
    @Test
    public void testScanning() throws InterruptedException {
        doTest(true);
    }
    public void doTest(boolean scanning) throws InterruptedException {

        final Bytes<?> b = Bytes.allocateElasticOnHeap();
        Wire wire = new TextWire(b) {
            @Override
            public boolean recordHistory() {
                return true;
            }
        };

        MyDtoListener myOut = wire.methodWriterBuilder(MyDtoListener.class).build();
        SnapshotListener snapshotOut = wire.methodWriterBuilder(SnapshotListener.class).build();

        generateHistory(1);
        myOut.myDto(new MyDto());

        generateHistory(2);
        snapshotOut.snapshot(new SnapshotDTO().data("one"));

        generateHistory(3);
        myOut.myDto(new MyDto());

        generateHistory(4);
        myOut.myDto(new MyDto());

        generateHistory(5);
        snapshotOut.snapshot(new SnapshotDTO().data("two"));

        generateHistory(6);
        myOut.myDto(new MyDto());

        SnapshotDTO[] q = {null};

        MethodReader reader = wire.methodReaderBuilder()
                .scanning(scanning)
                .build((SnapshotListener) d -> q[0] = d);

        if (!scanning) {
            // 1
            assertTrue(reader.readOne());
        }
        // 2
        assertTrue(reader.readOne());
        assertNotNull(q[0]);
        assertEquals("one", q[0].data);
        q[0] = null;

        if (!scanning) {
            // 3
            assertTrue(reader.readOne());
            // 4
            assertTrue(reader.readOne());
        }
        // 5
        assertTrue(reader.readOne());
        assertNotNull(q[0]);
        assertEquals("two", q[0].data);

        if (!scanning) {
            // 6
            assertTrue(reader.readOne());
        }
        assertFalse(reader.readOne());
    }

    @NotNull
    private VanillaMessageHistory generateHistory(int value) {
        VanillaMessageHistory messageHistory = (VanillaMessageHistory) MessageHistory.get();
        messageHistory.reset();
        messageHistory.addSource(value, value);
        return messageHistory;
    }

}
