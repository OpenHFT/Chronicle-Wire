/*
 * Copyright 2019-2022 - http://chronicle.software
 *
 * Chronicle software holds the rights to this software and it may not be redistributed to another organisation or a different team within your organisation.
 *
 * You may only use this software if you have prior written consent from Chronicle Software.
 *
 * This written consent may take the form of a valid (non expired) software licence.
 */
package net.openhft.chronicle.wire.trivial;

import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;
import net.openhft.chronicle.wire.converter.ShortText;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import static java.lang.Double.NaN;
import static net.openhft.chronicle.core.Jvm.fieldOffset;
import static net.openhft.chronicle.core.UnsafeMemory.UNSAFE;
import static net.openhft.chronicle.wire.ServicesTimestampLongConverter.INSTANCE;


/**
 * DTO for top N positions in an order book on a particular symbol.
 */
public class Orderbook extends BytesInBinaryMarshallable {

    static final int DTO_LENGTH = TriviallyCopyableUtils.length(Orderbook.class);
    private static final int DTO_START = TriviallyCopyableUtils.start(Orderbook.class);


    protected int fieldStart() {
        return DTO_START;
    }


    protected int fieldLength() {
        return DTO_LENGTH;
    }

    public static final int MAX_NUMBER_OF_BIDS = 10;
    public static final int MAX_NUMBER_OF_ASKS = 10;

    private static final long START_OF_BIDS_PRICE = fieldOffset(Orderbook.class, "bidPrice0");
    private static final long START_OF_BIDS_QTY = fieldOffset(Orderbook.class, "bidQty0");
    private static final long START_OF_ASKS_PRICE = fieldOffset(Orderbook.class, "askPrice0");
    private static final long START_OF_ASKS_QTY = fieldOffset(Orderbook.class, "askQty0");

    @LongConversion(ServicesTimestampLongConverter.class)
    private long eventTime;

    @ShortText
    public long symbol;

    @ShortText
    public long exchange;

    // bid
    public int bidCount;

    public double bidPrice0, bidPrice1, bidPrice2, bidPrice3, bidPrice4, bidPrice5, bidPrice6, bidPrice7, bidPrice8, bidPrice9;
    public double bidQty0, bidQty1, bidQty2, bidQty3, bidQty4, bidQty5, bidQty6, bidQty7, bidQty8, bidQty9;

    // ask
    public int askCount;
    public double askPrice0, askPrice1, askPrice2, askPrice3, askPrice4, askPrice5, askPrice6, askPrice7, askPrice8, askPrice9;
    public double askQty0, askQty1, askQty2, askQty3, askQty4, askQty5, askQty6, askQty7, askQty8, askQty9;

    public boolean asksChanged;
    public boolean bidsChanged;

    public double bidEndOfBook() {
        if (bidCount < MAX_NUMBER_OF_BIDS)
            return 0;
        return getBidPrice(bidCount - 1);
    }

    public double askEndOfBook() {
        if (askCount < MAX_NUMBER_OF_ASKS)
            return Long.MAX_VALUE;
        return getAskPrice(askCount - 1);
    }


    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {

        eventTime(INSTANCE.parse(wire.read("eventTime").text()));
        symbol = readAsLong(wire, "symbol");
        exchange = readAsLong(wire, "exchange");

        // why do this have to be at the start of the method ?
        askCount = readRungs(wire, "ask", this::setAskRungs0_9);
        bidCount = readRungs(wire, "bid", this::setBidRungs0_9);
    }

    public void eventTime(long eventTime) {
        this.eventTime = eventTime;
    }

    private int readRungs(@NotNull WireIn wire, final String side, Consumer<List<Rung>> setRungs) {
        final List<Rung> rungs = new ArrayList<>();
        wire.read(side).sequence(this, (t, v) -> {
            rungs.clear();
            while (v.hasNextSequenceItem()) {
                Rung r = new Rung();
                v.marshallable(r);
                rungs.add(r);
            }
        });
        setRungs.accept(rungs);
        return rungs.size();
    }


    public double getBidPrice(int index) {
        return getDoubleAtOffsetIndex(index, +START_OF_BIDS_PRICE);
    }

    public double getBidQty(int index) {
        return getDoubleAtOffsetIndex(index, START_OF_BIDS_QTY);
    }


    public double getAskPrice(int index) {
        return getDoubleAtOffsetIndex(index, START_OF_ASKS_PRICE);
    }

    public double getAskQty(int index) {
        return getDoubleAtOffsetIndex(index, START_OF_ASKS_QTY);
    }

    private double getDoubleAtOffsetIndex(int index, final long address) {
        return UNSAFE.getDouble(this, address + ((long) index << 3));
    }

    private Orderbook setDoubleAtOffsetIndex(int index, double value, final long address) {
        UNSAFE.putDouble(this, address + ((long) index << 3), value);
        return this;
    }

    public Orderbook setBidPrice(int index, double price) {
        return setDoubleAtOffsetIndex(index, price, START_OF_BIDS_PRICE);
    }

    public Orderbook setBidQty(int index, double qty) {
        return setDoubleAtOffsetIndex(index, qty, START_OF_BIDS_QTY);
    }

    public Orderbook setAskPrice(int index, double price) {
        return setDoubleAtOffsetIndex(index, price, START_OF_ASKS_PRICE);
    }

    public Orderbook setAskQty(int index, double qty) {
        return setDoubleAtOffsetIndex(index, qty, START_OF_ASKS_QTY);
    }

    public Orderbook symbol(long text) {
        this.symbol = text;
        return this;
    }

    public Orderbook symbol(CharSequence text) {
        this.symbol = ShortText.INSTANCE.parse(text);
        return this;
    }

    public Orderbook exchange(CharSequence text) {
        this.exchange = ShortText.INSTANCE.parse(text);
        return this;
    }

    public Orderbook exchange(long exchange) {
        this.exchange = exchange;
        return this;
    }

    public long exchange() {
        return this.exchange;
    }

    public long symbol() {
        return this.symbol;
    }

    private void setAskRungs0_9(Iterable<Rung> rungs) {
        int i = 0;
        for (Rung rung : rungs) {
            setAskPrice(i, rung.price).setAskQty(i, rung.qty);
            i++;
        }
        for (; i <= 9; i++) {
            setAskPrice(i, NaN).setAskQty(i, 0);
        }
    }

    private void setBidRungs0_9(Iterable<Rung> rungs) {
        int i = 0;
        for (Rung rung : rungs) {
            setDoubleAtOffsetIndex(i, rung.price, START_OF_BIDS_PRICE).setBidQty(i, rung.qty);
            i++;
        }
        for (; i <= 9; i++) {
            setDoubleAtOffsetIndex(i, NaN, START_OF_BIDS_PRICE).setBidQty(i, 0);
        }
    }

    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        wire.write("eventTime").text(INSTANCE.asString(eventTime));
        wire.write("symbol").text(ShortText.INSTANCE.asText(symbol));
        wire.write("exchange").text(ShortText.INSTANCE.asText(exchange));
        wire.write("bid").list(addBidRungs(bidCount), Rung.class);
        wire.write("ask").list(addAskRungs(askCount), Rung.class);
    }

    public long eventTime() {
        return this.eventTime;
    }

    private List<Rung> addBidRungs(int count) {
        final List<Rung> rungs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            rungs.add(new Rung(getBidPrice(i), (long) getBidQty(i)));
        }
        return rungs;
    }

    private List<Rung> addAskRungs(int count) {
        final List<Rung> rungs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            rungs.add(new Rung(getAskPrice(i), (long) getAskQty(i)));
        }
        return rungs;
    }

    private static long readAsLong(@NotNull WireIn wire, final String symbol) {
        final StringBuilder sb = Wires.acquireStringBuilderScoped().get();
        wire.read(symbol).text(sb);
        return ShortText.INSTANCE.parse(sb);
    }

    @Override
    public void reset() {
        super.reset();
        bidCount = 0;
        askCount = 0;
    }

    public void updateBids(TreeMap<Double, Double> bids) {
        int i = 0;
        for (Map.Entry<Double, Double> entry : bids.entrySet()) {
            if (i == MAX_NUMBER_OF_BIDS)
                break;
            setBidPrice(i, entry.getKey());
            setBidQty(i, entry.getValue());
            bidCount = ++i;
            bidsChanged = true;
        }
    }

    public void updateAsks(TreeMap<Double, Double> asks) {
        int i = 0;
        for (Map.Entry<Double, Double> entry : asks.entrySet()) {
            if (i == MAX_NUMBER_OF_BIDS)
                break;
            setAskPrice(i, entry.getKey());
            setAskQty(i, entry.getValue());
            askCount = ++i;
            asksChanged = true;
        }
    }

    /* */

    /**
     * Reads the binary representation of the object from a {@link BytesIn} instance,
     * effectively deserializing the object's state. This method relies on the Chronicle Bytes
     * library's unsafe operations for efficient, low-level data transfer.
     *
     * @param bytes The {@link BytesIn} instance to read from.
     * @throws IORuntimeException    If an IO error occurs during reading.
     * @throws IllegalStateException If the object cannot be read due to its state.
     */
    @Override
    public void readMarshallable(BytesIn<?> bytes) throws IORuntimeException, IllegalStateException {
        bytes.unsafeReadObject(this, fieldStart(), fieldLength());
    }

    /**
     * Writes the binary representation of the object to a {@link BytesOut} instance,
     * effectively serializing the object's state. This method uses the Chronicle Bytes
     * library's unsafe operations for efficient, low-level data writing.
     *
     * @param bytes The {@link BytesOut} instance to write to.
     * @throws IllegalStateException If the object cannot be written due to its state.
     */
    @Override
    public void writeMarshallable(BytesOut<?> bytes) throws IllegalStateException {
        bytes.unsafeWriteObject(this, fieldStart(), fieldLength());
    }

    @Override
    public boolean usesSelfDescribingMessage() {
        return super.usesSelfDescribingMessage();
    }
}
