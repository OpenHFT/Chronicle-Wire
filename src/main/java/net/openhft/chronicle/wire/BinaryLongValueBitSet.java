package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.ref.BinaryIntReference;
import net.openhft.chronicle.bytes.ref.BinaryLongReference;
import net.openhft.chronicle.bytes.ref.LongReference;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.values.IntValue;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Created by Rob Austin
 */
public class BinaryLongValueBitSet extends AbstractLongValueBitSet {

    /**
     * Creates a bit set using words as the internal representation.
     * The last word (if there is one) must be non-zero.
     *
     * @param words used to store the bitset
     */
    public BinaryLongValueBitSet(final LongReference[] words) {
        super(words);
    }

    @Override
    public void readMarshallable(@NotNull final WireIn wire) throws IORuntimeException {
        int numberOfLongValues = wire.read("numberOfLongValues").int32();

        Bytes<?> bytes = wire.bytes();
        BytesUtil.read8ByteAlignPadding(wire.bytes());

        wordsInUse = readBinaryIntReference(wire.bytes());

        BytesUtil.read8ByteAlignPadding(wire.bytes());

        words = new BinaryLongReference[numberOfLongValues];
        for (int i = 0; i < numberOfLongValues; i++) {

            // todo improve this so that it works with text wire
            BinaryLongReference ref = new BinaryLongReference();
            ref.bytesStore(Objects.requireNonNull(bytes.bytesStore()), bytes.readPosition(), 8);
            words[i] = ref;
            bytes.readSkip(8);
        }
    }

    @NotNull
    private BinaryIntReference readBinaryIntReference(Bytes<?> bytes) {
        BinaryIntReference result = new BinaryIntReference();
        result.bytesStore(Objects.requireNonNull(bytes.bytesStore()), bytes.readPosition(), 4);
        bytes.readSkip(4);
        return result;
    }

    @Override
    public void writeMarshallable(@NotNull final WireOut wire) {
        wire.write("numberOfLongValues").int32(words.length);

        Bytes<?> bytes = wire.bytes();
        BytesUtil.write8ByteAlignPadding(bytes);

        //wordsInUse
        wire.bytes().writeSkip(4);

        BytesUtil.write8ByteAlignPadding(bytes);

        // because this is a LongValue bit set the "words" are bound on the call to net.openhft.chronicle.wire.LongValueBitSet.readMarshallable
        wire.bytes().writeSkip(words.length * 8);
    }

    @NotNull
    protected IntValue newIntValue() {
        return new BinaryIntReference();
    }

}
