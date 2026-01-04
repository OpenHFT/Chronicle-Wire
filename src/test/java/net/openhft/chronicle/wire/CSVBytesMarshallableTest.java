/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.pool.EnumInterner;
import net.openhft.chronicle.core.scoped.ScopedResource;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static net.openhft.chronicle.wire.Wires.acquireStringBuilderScoped;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"deprecation", "removal"})
class CSVBytesMarshallableTest extends WireTestCommon {

    // Bytes representing raw data for the tests
    private final Bytes<?> bytes = Bytes.from(
            "1.09029,1.090305,EURUSD,2,1,EBS\n" +
                    "1.50935,1.50936,GBPUSD,5,1,RTRS\n" +
                    "1.0906,1.09065,EURCHF,3,1,EBS\n");

    // Test for low level bytes marshalling using FXPrice
    @Test
    @DisplayName("Serialises CSV bytes into raw bytes output")
    void bytesMarshallable() {
        Bytes<?> bytes2 = Bytes.allocateElasticOnHeap();
        @NotNull FXPrice fxPrice = new FXPrice();

        // Read, marshall, and write data from one set of bytes to another
        while (bytes.readRemaining() > 0) {
            fxPrice.readMarshallable(bytes);
            fxPrice.writeMarshallable(bytes2);
        }

        // Verify the resulting data
        assertEquals("1.09029,1.090305,EURUSD,2,EBS\n" +
                "1.50935,1.50936,GBPUSD,5,RTRS\n" +
                "1.0906,1.09065,EURCHF,3,EBS\n", bytes2.toString(),
                "Expected CSV bytes to round-trip without level field");
        bytes2.releaseLast();
    }

    // wire marshalling.
    @Test
    @DisplayName("Marshals CSV records through JSON wire")
    void marshallableJSON() {
        assertEquals(2, doTest(WireType.JSON, false), "Expected CSV record count for JSON wire");
    }

    @Test
    @DisplayName("Marshals CSV records through text wire")
    void marshallableTEXT() {
        assertEquals(2, doTest(WireType.TEXT, false), "Expected CSV record count for text wire");
    }

    @Test
    @DisplayName("Marshals CSV records through YAML wire")
    void marshallableYAML_ONLY() {
        assertEquals(2, doTest(WireType.YAML_ONLY, false), "Expected CSV record count for YAML wire");
    }

    @Test
    @DisplayName("Marshals CSV records through binary wire")
    void marshallableBINARY() {
        assertEquals(2, doTest(WireType.BINARY, true), "Expected CSV record count for binary wire");
    }

    @Test
    @DisplayName("Marshals CSV records through fieldless wire")
    void marshallableFIELDLESS() {
        assertEquals(2, doTest(WireType.FIELDLESS_BINARY, true),
                "Expected CSV record count for fieldless binary wire");
    }

    @Test
    @DisplayName("Marshals CSV records through raw wire")
    void marshallableRAW() {
        assertEquals(2, doTest(WireType.RAW, true), "Expected CSV record count for raw wire");
    }

    private int doTest(@NotNull WireType wt, boolean binary) {
        // Reset read position for input data
        bytes.readPosition(0);

        // Initialize wires for input and output data
        @NotNull CSVWire in = new CSVWire(bytes);

        Bytes<?> bytes2 = Bytes.allocateElasticOnHeap();
        Wire out = wt.apply(bytes2);

        @NotNull FXPrice2 fxPrice = new FXPrice2();
        int records = 0;

        // Read, marshall, and write data from one wire to another
        while (bytes.readRemaining() > 0) {
            fxPrice.readMarshallable(in);
            fxPrice.writeMarshallable(out);
            records++;
        }

        bytes2.releaseLast();
        return records;
    }
}

/**
 * Class representing a foreign exchange price.
 * Implements the BytesMarshallable interface to support reading and writing of its values from/to bytes.
 */
@SuppressWarnings({"rawtypes", "deprecation", "removal"})
class FXPrice implements BytesMarshallable {
    // Fields to store price data and related attributes
    private double bidprice;
    private double offerprice;
    //enum
    private CcyPair pair;
    private int size;
    private String exchangeName;

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
        Maths.toInt8(bytes.parseLong());
        exchangeName = bytes.parseUtf8(StopCharTesters.COMMA_STOP);
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
     * @param bytes    Source bytes
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
@SuppressWarnings({"deprecation", "removal"})
class FXPrice2 implements Marshallable {
    public transient double midPrice;
    // Fields to store price data and related attributes
    private double bidprice;
    private double offerprice;
    //enum
    private CcyPair pair;
    private int size;
    private byte level;
    private String exchangeName;

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
