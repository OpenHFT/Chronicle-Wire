package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Test;

import java.nio.ByteBuffer;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

/*
 * Created by dsmith on 11/20/16.
 */
public class WireBug39Test {
    @Test
    public void testBinaryEncoding() {
        @NotNull final WireType wireType = WireType.BINARY;
        @NotNull final String exampleString = "\uD83E\uDDC0 extra";

        @NotNull final MarshallableObj obj1 = new MarshallableObj();
        @NotNull final MarshallableObj obj2 = new MarshallableObj();
        @NotNull final MarshallableObj obj3 = new MarshallableObj();

        obj1.append(exampleString);
        obj2.append(exampleString);

        assertEquals("obj1.equals(obj2): ", obj1, obj2);

        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        obj2.writeMarshallable(wireType.apply(bytes));

        final String output = bytes.toString();
        System.out.println("output: [" + output + "]");

        obj3.readMarshallable(wireType.apply(Bytes.from(output)));

        assertEquals("obj2.equals(obj3): ", obj1, obj2);

        bytes.release();
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }

    class MarshallableObj implements Marshallable {
        private final StringBuilder builder = new StringBuilder();

        public void clear() {
            builder.setLength(0);
        }

        public void append(CharSequence cs) {
            builder.append(cs);
        }

        @Override
        public void readMarshallable(WireIn wire) throws IORuntimeException {
            builder.setLength(0);
            assertNotNull(wire.getValueIn().textTo(builder));
        }

        @Override
        public void writeMarshallable(WireOut wire) {
            wire.getValueOut().text(builder);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            @NotNull MarshallableObj that = (MarshallableObj) o;

            return builder.toString().equals(that.builder.toString());
        }

        @Override
        public int hashCode() {
            return builder.toString().hashCode();
        }

        @NotNull
        @Override
        public String toString() {
            return builder.toString();
        }
    }
}