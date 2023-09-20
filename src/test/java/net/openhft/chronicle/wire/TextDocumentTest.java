/*
 * Copyright 2016-2020 chronicle.software
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
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class TextDocumentTest extends WireTestCommon {

    // A helper function that performs the test on documents given a wireType.
    private static void doTestDocument(WireType wireType) {
        // Allocate an elastic byte buffer.
        @NotNull Bytes<?> bytes1 = Bytes.allocateElasticOnHeap();
        // Create a Wire object based on the provided wireType.
        @NotNull final Wire wire = wireType.apply(bytes1);
        // Create instances of the Header class for reading and writing.
        @NotNull final Header wheader = new Header();
        @NotNull final Header rheader = new Header();

        // Write the header to the wire.
        wire.writeDocument(true, w -> w.write(() -> "header").marshallable(wheader));

        // Check that the written bytes contain the expected serialized atomic values.
        @NotNull Bytes<?> bytes = wire.bytes();
        String actual = Wires.fromSizePrefixedBlobs(bytes);
        Assert.assertTrue(actual.contains(
                "  writeByte: !!atomic {  locked: false, value: 00000000000000000512 }"));
        Assert.assertTrue(actual.contains(
                "  readByte: !!atomic {  locked: false, value: 00000000000000001024 }"));

        // Read the header from the wire and populate rheader.
        wire.readDocument(w -> w.read(() -> "header").marshallable(rheader), null);

        // Assert that both the written and read headers are the same.
        assertEquals(wheader.uuid, rheader.uuid);
        assertEquals(wheader.created, rheader.created);

        // Close resources associated with the headers.
        wheader.closeAll();
        rheader.closeAll();
    }

    // Test the document writing and reading for TEXT wireType.
    @Test
    public void testDocument() {
        doTestDocument(WireType.TEXT);
    }

    // An ignored test for YAML_ONLY wireType. Needs to be fixed before running.
    @Ignore(/* TODO FIX */)
    @Test
    public void testDocumentYaml() {
        doTestDocument(WireType.YAML_ONLY);
    }

    // Enumeration of keys to use with the Wire objects.
    enum Keys implements WireKey {
        uuid,
        created,
        writeByte,
        readByte
    }

    // Nested class to represent a header, with functionality to write and read itself to/from a Wire.
    static class Header implements Marshallable {
        public static final long WRITE_BYTE = 512;
        public static final long READ_BYTE = 1024;

        UUID uuid;
        ZonedDateTime created;
        @Nullable
        LongValue writeByte;
        @Nullable
        LongValue readByte;

        // Constructor initializes a Header with random UUID and current date-time.
        public Header() {
            this.uuid = UUID.randomUUID();
            this.writeByte = null;
            this.readByte = null;
            this.created = ZonedDateTime.now();
        }

        // Serialize the object to a WireOut.
        @Override
        public void writeMarshallable(@NotNull WireOut out) {
            out.write(Keys.uuid).uuid(uuid);
            out.write(Keys.writeByte).int64forBinding(WRITE_BYTE);
            out.write(Keys.readByte).int64forBinding(READ_BYTE);
            out.write(Keys.created)
                    .zonedDateTime(created);
        }

        // Deserialize the object from a WireIn.
        @Override
        public void readMarshallable(@NotNull WireIn in) {
            in.read(Keys.uuid).uuid(this, (o, u) -> o.uuid = u);
            in.read(Keys.writeByte).int64(writeByte, this, (o, x) -> o.writeByte = x);
            in.read(Keys.readByte).int64(readByte, this, (o, x) -> o.readByte = x);
            in.read(Keys.created).zonedDateTime(this, (o, c) -> o.created = c);
        }

        // Close the resources associated with the header.
        public void closeAll() {
            Closeable.closeQuietly(readByte, writeByte);
        }
    }
}
