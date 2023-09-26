package run.chronicle;

import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.util.BinaryLengthLength;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * This outline what would happen under the covered.
 * This could be easily hidden from the user if they aren't interested in these details
 */
public class ExtendingWireTest {
    final Wire wire = WireType.BINARY.apply(new HexDumpBytes());

    /**
     * This added metadata before the message, ignored by the readers unless it's expecting this.
     */
    @Test
    public void prependingExcerpt() {
        wire.methodWriterBuilder(true, Additional.class).build()
                .added(new Added(0x12345678, "hi"));
        addMessage(wire);
        assertEquals("" +
                        "1c 00 00 40                                     # msg-length\n" +
                        "b9 05 61 64 64 65 64                            # added: (event)\n" +
                        "80 13                                           # Added\n" +
                        "   c4 63 73 75 6d                                  # csum:\n" +
                        "   a6 78 56 34 12                                  # 305419896\n" +
                        "   c5 6f 74 68 65 72                               # other:\n" +
                        "   e2 68 69                                        # hi\n" +
                        "18 00 00 00                                     # msg-length\n" +
                        "b9 03 64 74 6f                                  # dto: (event)\n" +
                        "80 11                                           # DTO\n" +
                        "   c3 6e 75 6d                                     # num:\n" +
                        "   a1 80                                           # 128\n" +
                        "   c4 74 65 78 74                                  # text:\n" +
                        "   e5 48 65 6c 6c 6f                               # Hello\n",
                wire.bytes().toHexString());
    }

    /**
     * This added metadata after the message, ignored by the readers unless it's expecting this.
     */
    @Test
    public void appendingExcerpt() {
        addMessage(wire);
        wire.methodWriterBuilder(true, Additional.class).build()
                .added(new Added(0x12345678, "hi"));
        assertEquals("" +
                        "18 00 00 00                                     # msg-length\n" +
                        "b9 03 64 74 6f                                  # dto: (event)\n" +
                        "80 11                                           # DTO\n" +
                        "   c3 6e 75 6d                                     # num:\n" +
                        "   a1 80                                           # 128\n" +
                        "   c4 74 65 78 74                                  # text:\n" +
                        "   e5 48 65 6c 6c 6f                               # Hello\n" +
                        "1c 00 00 40                                     # msg-length\n" +
                        "b9 05 61 64 64 65 64                            # added: (event)\n" +
                        "80 13                                           # Added\n" +
                        "   c4 63 73 75 6d                                  # csum:\n" +
                        "   a6 78 56 34 12                                  # 305419896\n" +
                        "   c5 6f 74 68 65 72                               # other:\n" +
                        "   e2 68 69                                        # hi\n",
                wire.bytes().toHexString());
    }

    /**
     * This added metadata before the message, this could be visible to MethodReaders if they had the right interface, otherwise ignored. c.f. AdditionalDTO
     */
    @Test
    public void prependingMessage() {
        AdditionalDTO build = wire.methodWriter(AdditionalDTO.class);
        addMessage(
                build
                        .added(new Added(0x12345678, "hi")));
        assertEquals("" +
                        "34 00 00 00                                     # msg-length\n" +
                        "b9 05 61 64 64 65 64                            # added: (event)\n" +
                        "80 13                                           # Added\n" +
                        "   c4 63 73 75 6d                                  # csum:\n" +
                        "   a6 78 56 34 12                                  # 305419896\n" +
                        "   c5 6f 74 68 65 72                               # other:\n" +
                        "   e2 68 69                                        # hi\n" +
                        "b9 03 64 74 6f                                  # dto: (event)\n" +
                        "80 11                                           # DTO\n" +
                        "   c3 6e 75 6d                                     # num:\n" +
                        "   a1 80                                           # 128\n" +
                        "   c4 74 65 78 74                                  # text:\n" +
                        "   e5 48 65 6c 6c 6f                               # Hello\n",
                wire.bytes().toHexString());
    }

    /**
     * This added metadata after the message, this could be visible to MethodReaders if they had the right interface, otherwise ignored. c.f. AdditionalDTO
     */
    @Test
    public void appendedMessage() {
        try (DocumentContext dc = wire.writingDocument()) {
            // these two messages could be combined into one interface if needed
            addMessage(wire);
            wire.methodWriter(Additional.class)
                    .added(new Added(0x12345678, "hi"));
        }
        assertEquals("" +
                        "34 00 00 00                                     # msg-length\n" +
                        "b9 03 64 74 6f                                  # dto: (event)\n" +
                        "80 11                                           # DTO\n" +
                        "   c3 6e 75 6d                                     # num:\n" +
                        "   a1 80                                           # 128\n" +
                        "   c4 74 65 78 74                                  # text:\n" +
                        "   e5 48 65 6c 6c 6f                               # Hello\n" +
                        "b9 05 61 64 64 65 64                            # added: (event)\n" +
                        "80 13                                           # Added\n" +
                        "   c4 63 73 75 6d                                  # csum:\n" +
                        "   a6 78 56 34 12                                  # 305419896\n" +
                        "   c5 6f 74 68 65 72                               # other:\n" +
                        "   e2 68 69                                        # hi\n",
                wire.bytes().toHexString());
    }

    /**
     * This added metadata after the message, this could be visible to MethodReaders if they had the right interface, otherwise ignored. c.f. AdditionalDTO
     */
    @Test
    public void secondMessage() {
        addMessage(wire);
        long id = wire.headerNumber();
        // much later by a different thread/process so the checksum can be processed offline
        wire.methodWriter(AdditionalID.class)
                .addId(new AddedWithId(0x12345678, "hi", id));

        assertEquals("" +
                        "18 00 00 00                                     # msg-length\n" +
                        "b9 03 64 74 6f                                  # dto: (event)\n" +
                        "80 11                                           # DTO\n" +
                        "   c3 6e 75 6d                                     # num:\n" +
                        "   a1 80                                           # 128\n" +
                        "   c4 74 65 78 74                                  # text:\n" +
                        "   e5 48 65 6c 6c 6f                               # Hello\n" +
                        "24 00 00 00                                     # msg-length\n" +
                        "b9 05 61 64 64 49 64                            # addId: (event)\n" +
                        "80 1b                                           # AddedWithId\n" +
                        "   c4 63 73 75 6d                                  # csum:\n" +
                        "   a6 78 56 34 12                                  # 305419896\n" +
                        "   c5 6f 74 68 65 72                               # other:\n" +
                        "   e2 68 69                                        # hi\n" +
                        "   c2 69 64                                        # id:\n" +
                        "   90 00 00 00 df                                  # -9.223372E18\n\n",
                wire.bytes().toHexString());
    }

    static void addMessage(Wire wire) {
        DTOPub pub = wire.methodWriter(DTOPub.class);
        addMessage(pub);
    }

    private static void addMessage(DTOPub pub) {
        pub.dto(new DTO(128, "Hello"));
    }

    interface Additional {
        // @MethodId('A') // to be more compact
        void added(Added added);
    }

    interface AdditionalID {
        // @MethodId('A') // to be more compact
        void addId(AddedWithId added);
    }

    interface AdditionalDTO {
        // @MethodId('a') // to be more compact
        DTOPub added(Added added);
    }

    // This could be BytesInBinaryMarshallable to be more compact
    static class Added extends SelfDescribingMarshallable {
        final long csum;
        final String other;

        public Added(long csum, String other) {
            this.csum = csum;
            this.other = other;
        }

        @Override
        public BinaryLengthLength binaryLengthLength() {
            return BinaryLengthLength.LENGTH_8BIT;
        }
    }

    static class AddedWithId extends Added {
        long id;

        public AddedWithId(long csum, String other, long id) {
            super(csum, other);
            this.id = id;
        }
    }

    interface DTOPub {
        void dto(DTO dto);
    }

    // This could be BytesInBinaryMarshallable to be more compact
    static class DTO extends SelfDescribingMarshallable {
        final int num;
        final String text;

        public DTO(int num, String text) {
            this.num = num;
            this.text = text;
        }

        @Override
        public BinaryLengthLength binaryLengthLength() {
            return BinaryLengthLength.LENGTH_8BIT;
        }
    }
}
