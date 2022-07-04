package net.openhft.chronicle.wire;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.FieldGroup;
import java.nio.ByteBuffer;
import static net.openhft.chronicle.bytes.Bytes.*;
import static net.openhft.chronicle.wire.WireType.*;
import net.openhft.chronicle.core.pool.ClassAliasPool;

public class LongConversionExampleC {
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(House.class);
    }
    public static class House extends SelfDescribingMarshallable {
        @FieldGroup("address")
        // 5 longs, each at 8 bytes = 40 bytes, so we can store a String with up to 39 ISO-8859 characters (as the first byte contains the length)
        private long text4a, text4b, text4c, text4d, text4e;
        private transient Bytes address = Bytes.forFieldGroup(this, "address");

        public void address(CharSequence owner) {
            address.append(owner);
        }
    }
    public static void main(String[] args) {
        House house = new House();
        house.address("82 St John Street, Clerkenwell, London");

        // creates a buffer to store bytes
        final Bytes<ByteBuffer> t = elasticHeapByteBuffer();

        // the encoding format
        final Wire wire = BINARY.apply(t);

        // writes the house object to the bytes
        wire.getValueOut().object(house);

        // dumps out the contents of the bytes
        System.out.println(t.toHexString());
        System.out.println(t);

        // reads the house object from the bytes
        final House object = wire.getValueIn().object(House.class);

        // prints the value of text4
        System.out.println(object.address);
    }
}
