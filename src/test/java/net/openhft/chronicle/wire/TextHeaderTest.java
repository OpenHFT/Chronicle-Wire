package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.Assert.assertEquals;


public class TextHeaderTest {

    enum MetaDataKey implements WireKey {
        header, index2index, index
    }

    enum Field implements WireKey {
        type,
        uuid, created, user, host, compression,
        indexCount, indexSpacing,
        writeByte, index2Index, lastIndex
    }

    private static class Header implements Marshallable {
        public static final long PADDED_SIZE = 512;

        private UUID uuid;
        private ZonedDateTime created;
        private LongValue writeByte;

        public Header(){
            this.uuid = UUID.randomUUID();
            this.writeByte = null;
            this.created = ZonedDateTime.now();
        }

        @Override
        public void writeMarshallable(@NotNull WireOut out) {
            out.write(Field.uuid).uuid(uuid);
            out.write(Field.writeByte).int64forBinding(PADDED_SIZE);
            out.write(Field.created).zonedDateTime(created);
        }

        @Override
        public void readMarshallable(@NotNull WireIn in) {
            System.out.println(in.bytes().toString());
            in.read(Field.uuid).uuid(u -> uuid = u);

            System.out.println(in.bytes().toString());
            in.read(Field.writeByte).int64(writeByte, x -> writeByte = x);

            System.out.println(in.bytes().toString());
            in.read(Field.created).zonedDateTime(c -> created = c);
        }
    }

    @Test
    public void testHeader() {
        Wire wire = new TextWire(NativeBytes.nativeBytes());
        Header wheader = new Header();
        Header rheader = new Header();

        wire.writeDocument(true, w -> w.write(() -> "header").marshallable(wheader));
        wire.flip();
        wire.readDocument(w -> w.read(() -> "header").marshallable(rheader), null);
    }
}