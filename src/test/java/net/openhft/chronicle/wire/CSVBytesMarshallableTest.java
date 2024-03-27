/*
 * Copyright 2016-2020 chronicle.software
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
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.pool.EnumInterner;
import net.openhft.chronicle.core.scoped.ScopedResource;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static net.openhft.chronicle.wire.Wires.acquireStringBuilderScoped;
import static org.junit.Assert.assertEquals;

// Enum representing currency pairs and an interner utility for its values
enum CcyPair {
    EURUSD, GBPUSD, EURCHF;

    // Static utility to intern the currency pair values
    static final EnumInterner<CcyPair> INTERNER = new EnumInterner<>(CcyPair.class);
}

public class CSVBytesMarshallableTest extends WireTestCommon {

    // Bytes representing raw data for the tests
    Bytes<?> bytes = Bytes.from(
            "1.09029,1.090305,EURUSD,2,1,EBS\n" +
                    "1.50935,1.50936,GBPUSD,5,1,RTRS\n" +
                    "1.0906,1.09065,EURCHF,3,1,EBS\n");

    // Test for low level bytes marshalling using FXPrice
    @Test
    public void bytesMarshallable() {
        Bytes<?> bytes2 = Bytes.elasticByteBuffer();
        @NotNull FXPrice fxPrice = new FXPrice();

        // Read, marshall, and write data from one set of bytes to another
        while (bytes.readRemaining() > 0) {
            fxPrice.readMarshallable(bytes);
            fxPrice.writeMarshallable(bytes2);
        }

        // Verify the resulting data
        assertEquals("1.09029,1.090305,EURUSD,2,EBS\n" +
                "1.50935,1.50936,GBPUSD,5,RTRS\n" +
                "1.0906,1.09065,EURCHF,3,EBS\n", bytes2.toString());
        bytes2.releaseLast();
    }

    // wire marshalling.
    @Test
    public void marshallableJSON() {
        doTest(WireType.JSON, false);
    }

    @Test
    public void marshallableTEXT() {
        doTest(WireType.TEXT, false);
    }

    @Test
    public void marshallableYAML_ONLY() {
        doTest(WireType.YAML_ONLY, false);
    }

    @Test
    public void marshallableBINARY() {
        doTest(WireType.BINARY, true);
    }

    @Test
    public void marshallableFIELDLESS() {
        doTest(WireType.FIELDLESS_BINARY, true);
    }

    @Test
    public void marshallableRAW() {
        doTest(WireType.RAW, true);
    }

    private void doTest(@NotNull WireType wt, boolean binary) {
        // Reset read position for input data
        bytes.readPosition(0);

        // Initialize wires for input and output data
        @NotNull CSVWire in = new CSVWire(bytes);

        Bytes<?> bytes2 = Bytes.elasticByteBuffer();
        Wire out = wt.apply(bytes2);

        @NotNull FXPrice2 fxPrice = new FXPrice2();

        // Read, marshall, and write data from one wire to another
        while (bytes.readRemaining() > 0) {
            fxPrice.readMarshallable(in);
            fxPrice.writeMarshallable(out);
        }

       // System.out.println();
       // System.out.println(wt);
       // System.out.println(binary ? bytes2.toHexString() : bytes2.toString());

        bytes2.releaseLast();
    }
}
/**
 * Class representing a foreign exchange price.
 * Implements the BytesMarshallable interface to support reading and writing of its values from/to bytes.
 */
@SuppressWarnings("rawtypes")
class FXPrice implements BytesMarshallable {
    // Fields to store price data and related attributes
    public double bidprice;
    public double offerprice;
    //enum
    public CcyPair pair;
    public int size;
    public byte level;
    public String exchangeName;
    public transient double midPrice;

    /**
     * Reads the object's data from bytes.
     *
     * @param bytes Source bytes
     */
    @Override
    public void readMarshallable(@NotNull BytesIn<?> bytes) {
        bidprice = bytes.parseDouble();
        offerprice = bytes.parseDouble();
        pair = parseEnum(bytes, CcyPair.INTERNER);
        size = Maths.toInt32(bytes.parseLong());
        level = Maths.toInt8(bytes.parseLong());
        exchangeName = bytes.parseUtf8(StopCharTesters.COMMA_STOP);
        midPrice = (bidprice + offerprice) / 2;
    }

    /**
     * Writes the object's data to bytes.
     *
     * @param bytes Target bytes
     */
    @Override
    public void writeMarshallable(@NotNull BytesOut<?> bytes) {
        bytes.append(bidprice).append(',');
        bytes.append(offerprice).append(',');
        bytes.append(pair.name()).append(',');
        bytes.append(size).append(',');
        bytes.append(exchangeName).append('\n');
    }

    /**
     * Helper method to parse an enum from bytes using an interner.
     *
     * @param bytes Source bytes
     * @param interner The enum interner to use for parsing
     * @return Parsed enum value
     */
    private <T extends Enum<T>> T parseEnum(@NotNull BytesIn<?> bytes, @NotNull EnumInterner<T> interner) {
        try (ScopedResource<StringBuilder> stlSb = acquireStringBuilderScoped()) {
            StringBuilder sb = stlSb.get();
            bytes.parseUtf8(sb, StopCharTesters.COMMA_STOP);
            return interner.intern(sb);
        }
    }
}

/**
 * Class representing a foreign exchange price.
 * Implements the Marshallable interface to support reading and writing of its values using the Wire format.
 */
class FXPrice2 implements Marshallable {
    // Fields to store price data and related attributes
    public double bidprice;
    public double offerprice;
    //enum
    public CcyPair pair;
    public int size;
    public byte level;
    public String exchangeName;
    public transient double midPrice;

    /**
     * Reads the object's data using the Wire format.
     *
     * @param wire Source wire
     */
    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        wire.read(() -> "bidprice").float64(this, (t, v) -> t.bidprice = v)
                .read(() -> "offerprice").float64(this, (t, v) -> t.offerprice = v)
                .read(() -> "pair").asEnum(CcyPair.class, this, (t, v) -> t.pair = v)
                .read(() -> "size").int32(this, (t, v) -> t.size = v)
                .read(() -> "level").int8(this, (t, v) -> t.level = v)
                .read(() -> "exchangeName").text(this, (t, v) -> t.exchangeName = v);
    }

    /**
     * Writes the object's data using the Wire format.
     *
     * @param wire Target wire
     */
    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        wire.write(() -> "bidprice").float64(bidprice)
                .write(() -> "offerprice").float64(offerprice)
                .write(() -> "pair").asEnum(pair)
                .write(() -> "size").int32(size)
                .write(() -> "level").int8(level)
                .write(() -> "exchangeName").text(exchangeName);
    }
}
