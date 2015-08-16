/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.wire.benchmarks.sbe;

import baseline.BooleanType;
import baseline.DataDecoder;
import baseline.DataEncoder;
import net.openhft.chronicle.wire.benchmarks.Data;
import net.openhft.chronicle.wire.benchmarks.Side;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

/**
 * Created by peter on 13/08/15.
 */
public class ExampleUsingGeneratedStub {
    public static void main(String[] args) {
        final Data data = new Data(123, 1234567890L, 1234, true, "Hello World", Side.Sell);
        DataEncoder de = new DataEncoder();
        UnsafeBuffer ub = new UnsafeBuffer(ByteBuffer.allocateDirect(128));
        int len = encode(de, ub, 0, data, ByteBuffer.allocate(64), new UnsafeBuffer(ByteBuffer.allocate(64)));
        System.out.println("len: " + len);
    }

    public static int encode(final DataEncoder de, final UnsafeBuffer directBuffer, final int bufferOffset, Data data, ByteBuffer textBuffer, MutableDirectBuffer buffer) {
        /*
            int smallInt = 0;
    long longInt = 0;
    double price = 0;
    boolean flag = false;
    StringBuilder text = new StringBuilder();
    Side side;

         */
        textBuffer.clear();
        data.copyTextTo(textBuffer);
        textBuffer.flip();
        de.wrap(directBuffer, bufferOffset)
                .price(data.getPrice())
                .smallInt(data.getSmallInt())
                .longInt(data.getLongInt())
                .flag(data.isFlag() ? BooleanType.TRUE : BooleanType.FALSE)
                .side(data.getSide() == net.openhft.chronicle.wire.benchmarks.Side.Buy ? baseline.Side.Buy : baseline.Side.Sell)
                .text().write(data.textAsBytes());

        return de.encodedLength();
    }

    public static void decode(
            final DataDecoder dd,
            final UnsafeBuffer directBuffer,
            final int bufferOffset,
            final int actingBlockLength,
            final int schemaId,
            final int actingVersion,
            Data data) {
        dd.wrap(directBuffer, bufferOffset, actingBlockLength, actingVersion);
        data.setPrice(dd.price());
        data.setSmallInt(dd.smallInt());
        data.setLongInt(dd.longInt());
        data.setFlag(dd.flag() == BooleanType.TRUE);
        data.setSide(dd.side() == baseline.Side.Buy ? Side.Buy : Side.Sell);
        dd.text().read(data.textAsBytes());
    }
}
