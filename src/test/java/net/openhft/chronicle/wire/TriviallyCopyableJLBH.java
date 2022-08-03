/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.util.BinaryLengthLength;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.jlbh.JLBH;
import net.openhft.chronicle.jlbh.JLBHOptions;
import net.openhft.chronicle.jlbh.JLBHTask;
import net.openhft.affinity.AffinityLock;
import org.jetbrains.annotations.NotNull;

import static net.openhft.chronicle.core.io.IOTools.deleteDirWithFiles;
import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;

public class TriviallyCopyableJLBH implements JLBHTask {


    enum HouseType {
        TRIVIALLY_COPYABLE, 
        BINARY_WIRE,
        UNKNOWN;
    }


    // use -Dio.type=binary or trivial to set. Defaults to binary
    HouseType type = HouseType.UNKNOWN;

    // use -Dcpu=N to set. Defaults to 2
    final int CPU;

    static {
        System.setProperty("jvm.resource.tracing", "false");
        CLASS_ALIASES.addAlias(TriviallyCopyableHouse.class, "House1");
        CLASS_ALIASES.addAlias(House.class, "House2");
        System.setProperty("disable.thread.safety", "true");
        System.setProperty("jvm.resource.tracing", "false");
        System.setProperty("check.thread.safety", "false");
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


        final Bytes address = Bytes.allocateDirect(128);

        public BaseHouse address(CharSequence owner) {
            address.clear().append(owner);
            return this;
        }
    }


    private JLBH lth;
    final BaseHouse originalHouse;

    private BaseHouse newHouse(HouseType type) {
        return type == HouseType.TRIVIALLY_COPYABLE ? new TriviallyCopyableHouse() : new House();
    }

    final BaseHouse targetHouse;

    final Wire wire = WireType.BINARY.apply(Bytes.allocateDirect(1024));

    public static void main(String[] args) {
        new TriviallyCopyableJLBH().test();   
        System.exit(0);
    }

    TriviallyCopyableJLBH() 
    {
        String ioType = System.getProperty("io.type", "binary");
        if(ioType.equals("binary")) {
            type = HouseType.BINARY_WIRE;
        } else if(ioType.equals("trivial")) {
            type = HouseType.TRIVIALLY_COPYABLE;
        } else {
            System.out.println("-Dio.type must be \"binary\" or \"trivial\"");
            System.exit(-1);
        }

        CPU = Integer.parseInt(System.getProperty("cpu", "2"));

        originalHouse = newHouse(type).address("82 St John Street, Clerkenwell");
        targetHouse = newHouse(type);
    }


    public void test() {
        deleteDirWithFiles("tmp");
        @NotNull JLBHOptions jlbhOptions = new JLBHOptions()
                .warmUpIterations(1_000_000)
                .iterations(1_000_000)
                .throughput(100_000)
                .accountForCoordinatedOmission(false)
                .acquireLock(()->AffinityLock.acquireLock(CPU))
                .recordOSJitter(false)
                .runs(5)
                .jlbhTask(this);
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
