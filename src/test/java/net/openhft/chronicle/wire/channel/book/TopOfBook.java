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

package net.openhft.chronicle.wire.channel.book;

import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.util.BinaryLengthLength;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.Base85LongConverter;
import net.openhft.chronicle.wire.BytesInBinaryMarshallable;
import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.converter.NanoTime;

public class TopOfBook extends BytesInBinaryMarshallable implements Cloneable {
    // some static variables related to byte-size of serialized objects
    static final int START_BYTES = BytesUtil.triviallyCopyableStart(TopOfBook.class);
    static final int LENGTH_BYTES = BytesUtil.triviallyCopyableLength(TopOfBook.class);

    // instance variables with various annotations, presumably for serialization customization
    @NanoTime
    private long sendingTimeNS;
    @LongConversion(Base85LongConverter.class)
    private long symbol; // up to 10 chars
    @LongConversion(Base85LongConverter.class)
    private int ecn; // up to 5 chars

    private double bidPrice, askPrice;
    private int bidQuantity, askQuantity;

    // 100 bytes
//    private double a6, a7, a8, a9, a10, a11, a12, a13;

    // 256
//    private int x;
//    private double b0, b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12, b13, b14, b15, b16, b17;

    @Override
    public final void readMarshallable(BytesIn bytes) throws IORuntimeException {
        bytes.unsafeReadObject(this, START_BYTES, LENGTH_BYTES);
    }

    @Override
    public final void writeMarshallable(BytesOut bytes) {
        bytes.unsafeWriteObject(this, START_BYTES, LENGTH_BYTES);
    }

    // setter method with fluent API design, and returning the object itself
    public TopOfBook sendingTimeNS(long sendingTimeNS) {
        this.sendingTimeNS = sendingTimeNS;
        return this;
    }

    // getter method without corresponding setter
    public long sendingTimeNS() {
        return sendingTimeNS;
    }

    // another getter and setter with fluent API design
    public long symbol() {
        return symbol;
    }

    public TopOfBook symbol(long symbol) {
        this.symbol = symbol;
        return this;
    }

    // and some more getter and setter methods
    public int ecn() {
        return ecn;
    }

    public TopOfBook ecn(int ecn) {
        this.ecn = ecn;
        return this;
    }

    // Retrieve the bid price
    public double bidPrices() {
        return bidPrice;
    }

    // Update the bid price and return the updated TopOfBook instance
    public TopOfBook bidPrices(double bidPrices) {
        this.bidPrice = bidPrices;
        return this;
    }

    // Retrieve the ask price
    public double askPrice() {
        return askPrice;
    }

    // Update the ask price and return the updated TopOfBook instance
    public TopOfBook askPrice(double askPrice) {
        this.askPrice = askPrice;
        return this;
    }

    // Retrieve the bid quantity
    public int bidQuantity() {
        return bidQuantity;
    }

    // Update the bid quantity and return the updated TopOfBook instance
    public TopOfBook bidQuantity(int bidQuantity) {
        this.bidQuantity = bidQuantity;
        return this;
    }

    // Retrieve the ask quantity
    public int askQuantity() {
        return askQuantity;
    }

    // Update the ask quantity and return the updated TopOfBook instance
    public TopOfBook askQuantity(int askQuantity) {
        this.askQuantity = askQuantity;
        return this;
    }

    // Create and return a deep copy of the current TopOfBook instance
    @Override
    public TopOfBook deepCopy() {
        TopOfBook tob = new TopOfBook();
        tob.symbol = symbol;
        tob.ecn = ecn;
        tob.sendingTimeNS = sendingTimeNS;
        tob.askPrice = askPrice;
        tob.askQuantity = askQuantity;
        tob.bidPrice = bidPrice;
        tob.bidQuantity = bidQuantity;
        return tob;
    }

    // Specify the binary length format as 8-bit for the TopOfBook instance
    @Override
    public BinaryLengthLength binaryLengthLength() {
        return BinaryLengthLength.LENGTH_8BIT;
    }
}
