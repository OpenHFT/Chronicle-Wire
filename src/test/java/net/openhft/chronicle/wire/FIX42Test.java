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

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;

/**
 * Created by peter on 06/10/15.
 */
@RunWith(value = Parameterized.class)
public class FIX42Test {
    final int testId;
    final boolean fixed;
    final boolean numericField;
    final boolean fieldLess;
    @NotNull
    Bytes bytes = nativeBytes();

    public FIX42Test(int testId, boolean fixed, boolean numericField, boolean fieldLess) {
        this.testId = testId;
        this.fixed = fixed;
        this.numericField = numericField;
        this.fieldLess = fieldLess;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        return Arrays.asList(
                new Object[]{-1, false, false, false},
                new Object[]{0, false, false, false},
                new Object[]{1, true, false, false},
                new Object[]{2, false, true, false},
                new Object[]{3, true, true, false},
                new Object[]{4, false, false, true},
                new Object[]{5, true, false, true}
        );
    }

    @NotNull
    private Wire createWire() {
        bytes.clear();
        if (testId < 0)
            return new TextWire(bytes);
        return new BinaryWire(bytes, fixed, numericField, fieldLess);
    }

    @Test
    public void dump() {
        Wire wire = createWire();
        MarketDataSnapshot mds = new MarketDataSnapshot("EURUSD", 1.1187, 1.1179);
        mds.writeMarshallable(wire);
        System.out.println(wire.getClass().getSimpleName() + ", fixed=" + fixed + ", numericField=" + numericField + ", fieldLess=" + fieldLess);
        if (wire instanceof TextWire)
            System.out.println(wire.bytes());
        else
            System.out.println(wire.bytes().toHexString());
    }


    static class MarketDataSnapshot implements WriteMarshallable {
        String symbol;
        double openingPrice, closingPrice;

        public MarketDataSnapshot(String symbol, double openingPrice, double closingPrice) {
            this.symbol = symbol;
            this.openingPrice = openingPrice;
            this.closingPrice = closingPrice;
        }

        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            wire.write(FIX42.Symbol).text(symbol)
                    .write(FIX42.NoMDEntries).int32(2)
                    .write(FIX42.MDEntryType).uint8('4')
                    .write(FIX42.MDEntryPx).float64(openingPrice)
                    .write(FIX42.MDEntryType).uint8('5')
                    .write(FIX42.MDEntryPx).float64(closingPrice);
        }
    }
}
