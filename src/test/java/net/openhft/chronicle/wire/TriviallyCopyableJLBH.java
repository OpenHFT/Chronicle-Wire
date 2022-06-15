package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.util.BinaryLengthLength;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.jlbh.JLBH;
import net.openhft.chronicle.jlbh.JLBHOptions;
import net.openhft.chronicle.jlbh.JLBHTask;
import org.jetbrains.annotations.NotNull;

import static net.openhft.chronicle.core.io.IOTools.deleteDirWithFiles;
import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;

public class TriviallyCopyableJLBH implements JLBHTask {


    enum HouseType {
        TRIVIALLY_COPYABLE, BINARY_WIRE;
    }


    // change these, to select which one to run
    HouseType type = HouseType.BINARY_WIRE;

    // HouseType type = HouseType.TRIVIALLY_COPYABLE;


    static {
        System.setProperty("jvm.resource.tracing", "false");
        CLASS_ALIASES.addAlias(TriviallyCopyableHouse.class, "House1");
        CLASS_ALIASES.addAlias(House.class, "House2");
        Jvm.isDebug();
        System.out.println("jvm.resource.tracing=" + System.getProperty("jvm.resource.tracing"));
        System.setProperty("dumpCode", "true");
    }

    public interface BaseHouse {
        BaseHouse address(CharSequence owner);

    }

    /**
     * Using Trivially Copyable Objects To Improve Java Serialisation Speeds
     */
    public static class TriviallyCopyableHouse extends BytesInBinaryMarshallable implements BaseHouse {

        @FieldGroup("address")
        // 5 longs, each at 8 bytes = 40 bytes, so we can store a String with up to 40 ISO-8859 characters
        private long text4a, text4b, text4c, text4d, text4e;
        private transient Bytes<?> address = Bytes.forFieldGroup(this, "address");

        private static final int[] START_END = BytesUtil.triviallyCopyableRange(TriviallyCopyableHouse.class);
        private static final int START = START_END[0];
        private static final int LENGTH = START_END[1] - START_END[0];

        public BaseHouse address(CharSequence owner) {
            address.clear().append(owner);
            return this;
        }


        // reads the bytes and updates 'this' instance, with the data in the bytes.
        @Override
        public void readMarshallable(BytesIn<?> bytes) {
            bytes.unsafeReadObject(this, START, LENGTH);
        }


        // reads 'this' instance and writes a copy of it to the bytes.
        @Override
        public void writeMarshallable(BytesOut<?> bytes) {
            bytes.unsafeWriteObject(this, START, LENGTH);
        }


        // the amount of data data in `this`
        @Override
        public BinaryLengthLength binaryLengthLength() {
            return BinaryLengthLength.LENGTH_8BIT;
        }


    }

    public static class House extends SelfDescribingMarshallable implements BaseHouse {

        final Bytes address = Bytes.elasticByteBuffer();

        public BaseHouse address(CharSequence owner) {
            address.clear().append(owner);
            return this;
        }
    }


    private JLBH lth;
    final BaseHouse originalHouse = newHouse(type).address("82 St John Street, Clerkenwell");

    private BaseHouse newHouse(HouseType type) {
        return type == HouseType.TRIVIALLY_COPYABLE ? new TriviallyCopyableHouse() : new House();
    }

    final BaseHouse targetHouse = new TriviallyCopyableHouse();

    final Wire wire = WireType.BINARY.apply(Bytes.elasticHeapByteBuffer());

    public static void main(String[] args) {
        deleteDirWithFiles("tmp");
        @NotNull JLBHOptions jlbhOptions = new JLBHOptions()
                .warmUpIterations(500_000)
                .iterations(500_000)
                .throughput(500_000)
                .accountForCoordinatedOmission(false)
                .runs(10)
                .jlbhTask(new TriviallyCopyableJLBH());
        new JLBH(jlbhOptions).start();
    }

    @Override
    public void run(long startTimeNS) {
        try {
            wire.bytes().clear();
            wire.getValueOut().object(originalHouse);
            wire.getValueIn().object(targetHouse, houseType(type));
            lth.sample(System.nanoTime() - startTimeNS);
        } catch (Exception e) {
            Jvm.rethrow(e);
        }

    }

    @NotNull
    private Class<? extends BaseHouse> houseType(HouseType type) {
        return type == HouseType.TRIVIALLY_COPYABLE ? TriviallyCopyableHouse.class : House.class;
    }

    @Override
    public void init(JLBH lth) {
        this.lth = lth;
    }
}
