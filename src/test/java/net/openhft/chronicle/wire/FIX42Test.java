/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;
/* This test prints

TextWire, fixed=false, numericField=false, fieldLess=false
symbol: EURUSD
NoMDEntries: 2
MDEntryType: 52
MDEntryPx: 1.1187
MDEntryType: 53
MDEntryPx: 1.1179

BinaryWire, fixed=false, numericField=false, fieldLess=false
00000000 C6 53 79 6D 62 6F 6C E6  45 55 52 55 53 44 CB 4E ·Symbol· EURUSD·N
00000010 6F 4D 44 45 6E 74 72 69  65 73 02 CB 4D 44 45 6E oMDEntri es··MDEn
00000020 74 72 79 54 79 70 65 34  C9 4D 44 45 6E 74 72 79 tryType4 ·MDEntry
00000030 50 78 91 2E 90 A0 F8 31  E6 F1 3F CB 4D 44 45 6E Px·.···1 ··?·MDEn
00000040 74 72 79 54 79 70 65 35  C9 4D 44 45 6E 74 72 79 tryType5 ·MDEntry
00000050 50 78 91 A5 2C 43 1C EB  E2 F1 3F                Px··,C·· ··?

BinaryWire, fixed=true, numericField=false, fieldLess=false
00000000 C6 53 79 6D 62 6F 6C E6  45 55 52 55 53 44 CB 4E ·Symbol· EURUSD·N
00000010 6F 4D 44 45 6E 74 72 69  65 73 A6 02 00 00 00 CB oMDEntri es······
00000020 4D 44 45 6E 74 72 79 54  79 70 65 A1 34 C9 4D 44 MDEntryT ype·4·MD
00000030 45 6E 74 72 79 50 78 91  2E 90 A0 F8 31 E6 F1 3F EntryPx· .···1··?
00000040 CB 4D 44 45 6E 74 72 79  54 79 70 65 A1 35 C9 4D ·MDEntry Type·5·M
00000050 44 45 6E 74 72 79 50 78  91 A5 2C 43 1C EB E2 F1 DEntryPx ··,C····
00000060 3F                                               ?

BinaryWire, fixed=false, numericField=true, fieldLess=false
00000000 BA 37 E6 45 55 52 55 53  44 BA 8C 02 02 BA 8D 02 ·7·EURUS D·······
00000010 34 BA 8E 02 91 2E 90 A0  F8 31 E6 F1 3F BA 8D 02 4····.·· ·1··?···
00000020 35 BA 8E 02 91 A5 2C 43  1C EB E2 F1 3F          5·····,C ····?

BinaryWire, fixed=true, numericField=true, fieldLess=false
00000000 BA 37 E6 45 55 52 55 53  44 BA 8C 02 A6 02 00 00 ·7·EURUS D·······
00000010 00 BA 8D 02 A1 34 BA 8E  02 91 2E 90 A0 F8 31 E6 ·····4·· ··.···1·
00000020 F1 3F BA 8D 02 A1 35 BA  8E 02 91 A5 2C 43 1C EB ·?····5· ····,C··
00000030 E2 F1 3F                                         ··?

BinaryWire, fixed=false, numericField=false, fieldLess=true
00000000 E6 45 55 52 55 53 44 02  34 91 2E 90 A0 F8 31 E6 ·EURUSD· 4·.···1·
00000010 F1 3F 35 91 A5 2C 43 1C  EB E2 F1 3F             ·?5··,C· ···?

BinaryWire, fixed=true, numericField=false, fieldLess=true
00000000 E6 45 55 52 55 53 44 A6  02 00 00 00 A1 34 91 2E ·EURUSD· ·····4·.
00000010 90 A0 F8 31 E6 F1 3F A1  35 91 A5 2C 43 1C EB E2 ···1··?· 5··,C···
00000020 F1 3F                                            ·?

 */

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;

/*
 * Created by Peter Lawrey on 06/10/15.
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
        @NotNull Wire wire = testId < 0
                ? new TextWire(bytes)
                : new BinaryWire(bytes, fixed, numericField, fieldLess, Integer.MAX_VALUE, "binary", true);
        assert wire.startUse();
        return wire;
    }

    @Test
    public void dump() {
        @NotNull Wire wire = createWire();
        @NotNull MarketDataSnapshot mds = new MarketDataSnapshot("EURUSD", 1.1187, 1.1179);
        mds.writeMarshallable(wire);
        System.out.println(wire.getClass().getSimpleName() + ", fixed=" + fixed + ", numericField=" + numericField + ", fieldLess=" + fieldLess);
        if (wire instanceof TextWire)
            System.out.println(wire.bytes());
        else
            System.out.println(wire.bytes().toHexString());
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
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
