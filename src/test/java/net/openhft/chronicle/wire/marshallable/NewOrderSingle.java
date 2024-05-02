package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.FieldGroup;
import net.openhft.chronicle.wire.*;
import net.openhft.chronicle.wire.marshallable.converter.*;
import org.jetbrains.annotations.NotNull;

import static net.openhft.chronicle.bytes.Bytes.forFieldGroup;
import static net.openhft.chronicle.wire.marshallable.converter.TimeInForceConverter.GOOD_TILL_CANCEL;

/**
 * The type New order single.
 */
public final class NewOrderSingle extends SelfDescribingTriviallyCopyable implements Event<NewOrderSingle>, Marshallable {

    public static int start(Class<?> c) {
        return BytesUtil.triviallyCopyableRange(c)[0];
    }

    public static int length(Class<?> c) {
        int[] BYTE_RANGE = BytesUtil.triviallyCopyableRange(c);
        return BYTE_RANGE[1] - BYTE_RANGE[0];
    }

    private static final int DTO_START = start(NewOrderSingle.class);
    private static final int DTO_LENGTH = length(NewOrderSingle.class);
    private static final int LATEST_FORMAT = 1;
    @LongConversion(ServicesTimestampLongConverter.class)
    private long eventTime;

    /**
     * Unique identifier for Order as assigned by institution. Uniqueness must be guaranteed within a single trading day.
     * Firms which electronically submit multi-day orders should consider embedding a date within the ClOrdID <11> field
     * to assure uniqueness across days.
     * <p>
     * see https://www.onixs.biz/fix-dictionary/4.0/tagnum_11
     */
    private final Bytes<?> clOrdID = forFieldGroup(this, "clOrdID");

    @Override
    public long eventTime() {
        return eventTime;
    }

    @FieldGroup("clOrdID")
    transient long clOrdID0, clOrdID1, clOrdID2, clOrdID3, clOrdID4;

    private final Bytes<?> reason = forFieldGroup(this, "reason");

    @FieldGroup("reason")
    transient long reason0, reason1, reason2, reason3, reason4, reason5, reason6, reason7, reason8,
            reason9, reason10, reason11, reason12,reason13,reason14,reason15,reason16;

    private final Bytes<?> expireDate = forFieldGroup(this, "expireDate");
    @FieldGroup("expireDate")
    transient long expireDate0, expireDate1;

    private final Bytes<?> partyID = forFieldGroup(this, "partyID");

    @FieldGroup("partyID")
    transient long partyID0, partyID1, partyID2, partyID3, partyID4, partyID5, partyID6, partyID7, partyID8, partyID9;

    public NewOrderSingle clOrdID(long clOrdID) {
        this.clOrdID.clear().append(clOrdID);
        return this;
    }

    /**
     * Ticker symbol. Common, "human understood" representation of the security. SecurityID <48> value can be specified
     * if no symbol exists (e.g. non-exchange traded Collective Investment Vehicles)
     * <p>
     * see tagNum_55
     */
    @LongConversion(Base85LongConverter.class)
    private long symbol;

    /**
     * Order type. see tagnum_40
     */
    @LongConversion(OrdTypeConverter.class)
    private char ordType = OrdTypeConverter.MARKET;

    @LongConversion(ExecInstConverter.class)
    private char execInst = ExecInstConverter.NONE;

    /**
     * Specifies how long the order remains in effect. Absence of this field is interpreted as DAY. NOTE not applicable to CIV Orders.
     * <p>
     * public final static char GOOD_TILL_CANCEL = '1';
     * public final static char IMMEDIATE_OR_CANCEL = '3';
     * public final static char FILL_OR_KILL = '4';
     * public final static char GOOD_TILL_DATE = '6';
     * <p>
     * tagnum_59
     */
    @LongConversion(TimeInForceConverter.class)
    private char timeInForce = GOOD_TILL_CANCEL;

    /**
     * Side of order.
     * <p>
     * Valid values:
     * <p>
     * 1 = Buy
     * <p>
     * 2 = Sell
     * <p>
     * tagnum_54
     */
    @LongConversion(SideConverter.class)
    private char side;

    /**
     * Quantity ordered. This represents the number of shares for equities or par, face or nominal value for FI instruments.
     * tagNum_38
     */
    @LongConversion(QtyLongConvertor.class)
    private long orderQty;


    /**
     * Price per unit of quantity (e.g. per share)
     * tagNum_44
     */
    private double price;


    /**
     * Time of execution/order creation (expressed in UTC (Universal Time Coordinated, also known as "GMT")
     * tagNum_60
     */
    @LongConversion(NanoTimestampLongConverter.class)
    private long transactTime;



    public Bytes partyID() {
        return partyID;
    }

    public NewOrderSingle partyID(CharSequence partyIDSource) {
        this.partyID.clear().append(partyIDSource);
        return this;
    }

    /**
     * @return Unique identifier for Order as assigned by institution. Uniqueness must be guaranteed within a single trading day.
     * Firms which electronically submit multi-day orders should consider embedding a date within the {@code ClOrdID <11>} field to assure uniqueness across days.
     */
    public @NotNull Bytes clOrdID() {
        return clOrdID;
    }


    public @NotNull NewOrderSingle clOrdID(@NotNull CharSequence clOrdID) {
        this.clOrdID.clear().append(clOrdID);
        return this;
    }


    /**
     * @return the Symbol
     */
    public long symbol() {
        return symbol;
    }

    /**
     * @param symbol the symbol
     * @return self
     */
    public @NotNull NewOrderSingle symbol(long symbol) {
        this.symbol = symbol;
        return this;
    }

    /**
     * @return Ord type char.
     */
    public char ordType() {
        return ordType;
    }

    public char execInst() {
        return execInst;
    }

    /**
     * @param ordType the ord type
     * @return self
     */
    public @NotNull NewOrderSingle ordType(char ordType) {
        this.ordType = ordType;
        return this;
    }

    public @NotNull NewOrderSingle execInst(char execInst) {
        this.execInst = execInst;
        return this;
    }

    /**
     * @return the Time in force
     */
    public char timeInForce() {
        return timeInForce;
    }

    /**
     * @param timeInForce Time in force
     * @return self
     */
    @NotNull
    public NewOrderSingle timeInForce(char timeInForce) {
        this.timeInForce = timeInForce;
        return this;
    }

    /**
     * @return the Expire date time
     */
    public CharSequence expireDate() {
        return expireDate;
    }

    /**
     * @param expireDate where the order will expire.
     *                   (Required if tag 59=6)
     * @return self
     */
    @NotNull
    public NewOrderSingle expireDate(Bytes expireDate) {
        this.expireDate.clear().append(expireDate);
        return this;
    }

    /**
     * @return the Side
     */
    public char side() {
        return side;
    }

    /**
     * @param side the side
     * @return self
     */
    public @NotNull NewOrderSingle side(char side) {
        this.side = side;
        return this;
    }

    /**
     * @return the Order qty
     */
    public long orderQty() {
        return orderQty;
    }

    /**
     * @param orderQty the order qty
     * @return self
     */
    public @NotNull NewOrderSingle orderQty(long orderQty) {
        this.orderQty = orderQty;
        return this;
    }


    public long transactTime() {
        return transactTime;
    }

    /**
     * @param transactTime the transact time
     * @return self
     */
    @NotNull
    public NewOrderSingle transactTime(long transactTime) {
        this.transactTime = transactTime;
        return this;
    }

    /**
     * Price long.
     *
     * @return the long
     */
    public double price() {
        return price;
    }


    /**
     * @param price the price
     * @return self
     */
    @NotNull
    public NewOrderSingle price(double price) {
        this.price = price;
        return this;
    }

    /**
     * @param symbol the symbol
     * @return self
     */
    @NotNull
    public NewOrderSingle symbol(CharSequence symbol) {
        this.symbol = Base85LongConverter.INSTANCE.parse(symbol);
        return this;
    }


    public long longClOrdId() {
        return Base32LongConverter.INSTANCE.parse(clOrdID);
    }

    public void reason(CharSequence reason) {
        this.reason.clear().append(reason);
    }

    @Override
    protected int $description() {
        return 0;
    }

    @Override
    protected int $start() {
        return DTO_START;
    }

    @Override
    protected int $length() {
        return DTO_LENGTH;
    }
}
