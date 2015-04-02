package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.Assert.assertEquals;


public class TextDocumentTest {
    enum Keys implements WireKey {
        uuid,
        created,
        writeByte
    }

    private static class Header implements Marshallable {
        public static final long PADDED_SIZE = 512;

        UUID uuid;
        ZonedDateTime created;
        LongValue writeByte;

        public Header(){
            this.uuid = UUID.randomUUID();
            this.writeByte = null;
            this.created = ZonedDateTime.now();
        }

        @Override
        public void writeMarshallable(@NotNull WireOut out) {
            out.write(Keys.uuid).uuid(uuid);
            out.write(Keys.writeByte).int64forBinding(PADDED_SIZE);
            out.write(Keys.created).zonedDateTime(created);
        }

        @Override
        public void readMarshallable(@NotNull WireIn in) {
            System.out.println("Before reading uuid: \n" + in.bytes().toString());
            in.read(Keys.uuid).uuid(u -> uuid = u);

            System.out.println("Before reading writeByte: \n" + in.bytes().toString());
            in.read(Keys.writeByte).int64(writeByte, x -> writeByte = x);

            System.out.println("Before reading created: \n" + in.bytes().toString());
            in.read(Keys.created).zonedDateTime(c -> created = c);
        }
    }

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
         at net.openhft.chronicle.wire.Wires.lambda$readData$7(Wires.java:79)
         at net.openhft.chronicle.wire.Wires$$Lambda$11/13492111.accept(Unknown Source)
         at net.openhft.chronicle.bytes.StreamingCommon.withLength(StreamingCommon.java:33)
         at net.openhft.chronicle.wire.Wires.readData(Wires.java:79)
         at net.openhft.chronicle.wire.WireIn.readDocument(WireIn.java:53)
         at net.openhft.chronicle.wire.TextDocumentTest.testDocument(TextDocumentTest.java:62)
     */
    @Ignore
    @Test
    public void testDocument() {
        Wire wire = new TextWire(NativeBytes.nativeBytes());
        Header wheader = new Header();
        Header rheader = new Header();

        wire.writeDocument(true, w -> w.write(() -> "header").marshallable(wheader));
        wire.flip();
        wire.readDocument(w -> w.read(() -> "header").marshallable(rheader), null);

        assertEquals(wheader.uuid, rheader.uuid);
        assertEquals(wheader.writeByte.getValue(), rheader.writeByte.getValue());
        assertEquals(wheader.created, rheader.created);
    }
}