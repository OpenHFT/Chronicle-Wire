package net.openhft.chronicle.wire.examples;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.*;

import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;

public class WireExamples2 {

    public static void main(String... args) {
        CLASS_ALIASES.addAlias(TextObject.class);
        final Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap());

        // serialize
        wire.getValueOut().object(new TextObject("SAMPLETEXT"));

        // log out the encoded data
        System.out.println("encoded to=" + wire.bytes().toHexString());

        // deserialize
        System.out.println("deserialized=" + wire.getValueIn().object());

    }

    public static class TextObject extends SelfDescribingMarshallable {
        transient StringBuilder temp = new StringBuilder();

        @LongConversion(Base64LongConverter.class)
        private long text;

        public TextObject(CharSequence text) {
            this.text = Base64LongConverter.INSTANCE.parse(text);
        }

        public CharSequence text() {
            Base64LongConverter.INSTANCE.append(temp, text);
            return temp;
        }
    }
}
