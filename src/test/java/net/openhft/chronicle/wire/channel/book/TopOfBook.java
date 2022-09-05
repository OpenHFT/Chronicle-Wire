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

public class TopOfBook extends BytesInBinaryMarshallable {
    static final int START_BYTES = BytesUtil.triviallyCopyableStart(TopOfBook.class);
    static final int LENGTH_BYTES = BytesUtil.triviallyCopyableLength(TopOfBook.class);
    @NanoTime
    private long sendingTimeNS;
    @LongConversion(Base85LongConverter.class)
    private long symbol; // up to 10 chars
    @LongConversion(Base85LongConverter.class)
    private int ecn; // up to 5 chars

    private double bidPrice, askPrice;
    private int bidQuantity, askQuantity;

    @Override
    public final void readMarshallable(BytesIn bytes) throws IORuntimeException {
        bytes.unsafeReadObject(this, START_BYTES, LENGTH_BYTES);
    }

    @Override
    public final void writeMarshallable(BytesOut bytes) {
        bytes.unsafeWriteObject(this, START_BYTES, LENGTH_BYTES);
    }

    public void sendingTimeNS(long sendingTimeNS) {
        this.sendingTimeNS = sendingTimeNS;
    }

    public long sendingTimeNS() {
        return sendingTimeNS;
    }

    public long symbol() {
        return symbol;
    }

    public TopOfBook symbol(long symbol) {
        this.symbol = symbol;
        return this;
    }

    public int ecn() {
        return ecn;
    }

    public TopOfBook ecn(int ecn) {
        this.ecn = ecn;
        return this;
    }

    public double bidPrices() {
        return bidPrice;
    }

    public TopOfBook bidPrices(double bidPrices) {
        this.bidPrice = bidPrices;
        return this;
    }

    public double askPrice() {
        return askPrice;
    }

    public TopOfBook askPrice(double askPrice) {
        this.askPrice = askPrice;
        return this;
    }

    public int bidQuantity() {
        return bidQuantity;
    }

    public TopOfBook bidQuantity(int bidQuantity) {
        this.bidQuantity = bidQuantity;
        return this;
    }

    public int askQuantity() {
        return askQuantity;
    }

    public TopOfBook askQuantity(int askQuantity) {
        this.askQuantity = askQuantity;
        return this;
    }

    @Override
    public BinaryLengthLength binaryLengthLength() {
        return BinaryLengthLength.LENGTH_8BIT;
    }
}
