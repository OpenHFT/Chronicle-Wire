/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class TextDocumentTest {
    /*
     Before reading uuid:
         uuid: 11d0c0c2-657d-4745-8be6-cc781cfc8279, writeByte: !!atomic { locked: false, value: 00000000000000000512 }, created:2015-04-02T09:30:15.134+02:00[Europe/Rome] }
     Before reading writeByte:
         writeByte: !!atomic { locked: false, value: 00000000000000000512 }, created:2015-04-02T09:30:15.134+02:00[Europe/Rome] }
     Before reading created:
         , created:2015-04-02T09:30:15.134+02:00[Europe/Rome] }

     java.lang.UnsupportedOperationException: Unordered fields not supported yet. key=created
         at net.openhft.chronicle.wire.TextWire.read(TextWire.java:133)
         at net.openhft.chronicle.wire.TextDocumentTest$Header.readMarshallable(TextDocumentTest.java:50)
         at net.openhft.chronicle.wire.TextWire$TextValueIn.marshallable(TextWire.java:653)
         at net.openhft.chronicle.wire.TextDocumentTest.lambda$testDocument$6(TextDocumentTest.java:62)
         at net.openhft.chronicle.wire.TextDocumentTest$$Lambda$10/21801535.accept(Unknown Source)
         at net.openhft.chronicle.wire.WireInternal.lambda$readData$7(WireInternal.java:79)
         at net.openhft.chronicle.wire.WireInternal$$Lambda$11/13492111.accept(Unknown Source)
         at net.openhft.chronicle.bytes.StreamingCommon.withLength(StreamingCommon.java:33)
         at net.openhft.chronicle.wire.WireInternal.readData(WireInternal.java:79)
         at net.openhft.chronicle.wire.WireIn.readDocument(WireIn.java:53)
         at net.openhft.chronicle.wire.TextDocumentTest.testDocument(TextDocumentTest.java:62)
     */
    @Test
    public void testDocument() {
        @NotNull Bytes<Void> bytes1 = Bytes.allocateElasticDirect();
        @NotNull final Wire wire = new TextWire(bytes1);
        @NotNull final Header wheader = new Header();
        @NotNull final Header rheader = new Header();

        wire.writeDocument(true, w -> w.write(() -> "header").marshallable(wheader));

        @NotNull Bytes<?> bytes = wire.bytes();
        assertEquals("--- !!meta-data\n" +
                "header: {\n" +
                "  uuid: " + wheader.uuid + ",\n" +
                "  writeByte: !!atomic { locked: false, value: 00000000000000000512 },\n" +
                "  readByte: !!atomic { locked: false, value: 00000000000000001024 },\n" +
                "  created: \"" + wheader.created + "\"\n" +
                "}\n", Wires.fromSizePrefixedBlobs(bytes));
        wire.readDocument(w -> w.read(() -> "header").marshallable(rheader), null);

        assertEquals(wheader.uuid, rheader.uuid);
        assertEquals(Header.WRITE_BYTE, rheader.writeByte.getValue());
        assertEquals(Header.READ_BYTE, rheader.readByte.getValue());
        assertEquals(wheader.created, rheader.created);
    }

    enum Keys implements WireKey {
        uuid,
        created,
        writeByte,
        readByte
    }

    private static class Header implements Marshallable {
        public static final long WRITE_BYTE = 512;
        public static final long READ_BYTE = 1024;

        UUID uuid;
        ZonedDateTime created;
        @Nullable
        LongValue writeByte;
        @Nullable
        LongValue readByte;

        public Header() {
            this.uuid = UUID.randomUUID();
            this.writeByte = null;
            this.readByte = null;
            this.created = ZonedDateTime.now();
        }

        @Override
        public void writeMarshallable(@NotNull WireOut out) {
            out.write(Keys.uuid).uuid(uuid);
            out.write(Keys.writeByte).int64forBinding(WRITE_BYTE);
            out.write(Keys.readByte).int64forBinding(READ_BYTE);
            out.write(Keys.created)
                    .zonedDateTime(created);
        }

        @Override
        public void readMarshallable(@NotNull WireIn in) {
            in.read(Keys.uuid).uuid(this, (o, u) -> o.uuid = u);
            in.read(Keys.writeByte).int64(writeByte, this, (o, x) -> o.writeByte = x);
            in.read(Keys.readByte).int64(readByte, this, (o, x) -> o.readByte = x);
            in.read(Keys.created).zonedDateTime(this, (o, c) -> o.created = c);
        }
    }
}