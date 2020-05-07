package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static net.openhft.chronicle.wire.WireMarshaller.WIRE_MARSHALLER_CL;

@SuppressWarnings("rawtypes")
public class FloatDtoTest {

    @Test
    public void test() {
        @NotNull final Value value = new Value(99, 2000f);
        final Bytes bytes = Wires.acquireBytes();
        final Wire w = WireType.BINARY.apply(bytes);
        w.write().marshallable(value);
        @NotNull Value object1 = new Value(0, 0.0f);
        w.read().marshallable(object1);
    }

    private static class Key extends SelfDescribingMarshallable implements
            KeyedMarshallable {
        @SuppressWarnings("unused")
        int uiid;

        Key(int uiid) {
            this.uiid = uiid;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void writeKey(@NotNull Bytes bytes) {
            WIRE_MARSHALLER_CL.get(Key.class).writeKey(this, bytes);
        }
    }

    private static class Value extends Key implements Marshallable {

        @SuppressWarnings("unused")
        final float myFloat;

        Value(int uiid,
              float myFloat) {
            super(uiid);
            this.myFloat = myFloat;
        }
    }

}
