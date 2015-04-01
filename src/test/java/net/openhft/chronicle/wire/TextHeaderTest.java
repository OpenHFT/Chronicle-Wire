package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.UUID;



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
        private static final long PADDED_SIZE = 512;

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
            out.write(Field.writeByte).int64forBinding(PADDED_SIZE)
                    .write(Field.created).zonedDateTime(created);
        }

        @Override
        public void readMarshallable(@NotNull WireIn in) {
            in.read(Field.writeByte).int64(writeByte, x -> writeByte = x)
                    .read(Field.created).zonedDateTime(c -> created = c);
        }
    }

    @Test
    public void testHeader() {
        Wire wire = new TextWire(NativeBytes.nativeBytes());
        Header header = new Header();

        wire.writeDocument(true, w -> w.write(() -> "header").marshallable(header));
        wire.flip();

        System.out.println(wire.bytes().toString());

        wire.readDocument(w -> w.read(() -> "header").marshallable(header), null);
    }
}