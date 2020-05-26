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
 */package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class TextDocumentTest {

    @Test
    public void testDocument() {
        @NotNull Bytes<Void> bytes1 = Bytes.allocateElasticDirect();
        @NotNull final Wire wire = new TextWire(bytes1);
        @NotNull final Header wheader = new Header();
        @NotNull final Header rheader = new Header();

        wire.writeDocument(true, w -> w.write(() -> "header").marshallable(wheader));

        @NotNull Bytes<?> bytes = wire.bytes();
        String actual = Wires.fromSizePrefixedBlobs(bytes);
        Assert.assertTrue(actual.contains(
                "  writeByte: !!atomic {  locked: false, value: 00000000000000000512 },\n" +
                        "  readByte: !!atomic {  locked: false, value: 00000000000000001024 }"));

        wire.readDocument(w -> w.read(() -> "header").marshallable(rheader), null);

        assertEquals(wheader.uuid, rheader.uuid);
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