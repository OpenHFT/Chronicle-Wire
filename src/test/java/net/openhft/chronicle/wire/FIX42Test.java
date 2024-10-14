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
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.bytes.Bytes.allocateElasticOnHeap;
import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class FIX42Test extends WireTestCommon {
    // Test ID for identification
    final int testId;

    // Flag to determine if the test is fixed
    final boolean fixed;

    // Flag to determine if the field is numeric
    final boolean numericField;

    // Flag to determine if the field is absent
    final boolean fieldLess;

    // Dump string for storing binary representations
    private final String dump;

    // Elastic byte buffer for writing and reading data
    @SuppressWarnings("rawtypes")
    @NotNull
    Bytes<?> bytes = allocateElasticOnHeap();

    // Constructor to initialize the test parameters
    public FIX42Test(int testId, boolean fixed, boolean numericField, boolean fieldLess, String dump) {
        this.testId = testId;
        this.fixed = fixed;
        this.numericField = numericField;
        this.fieldLess = fieldLess;
        this.dump = dump;
    }

    // Provides various combinations of parameters to run the test with
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> combinations() {
        // Various dump strings representing different binary data scenarios
        String dump_1 = "" +
                "Symbol: EURUSD\n" +
                "NoMDEntries: 2\n" +
                "MDEntryType: 52\n" +
                "MDEntryPx: 1.1187\n" +
                "MDEntryType: 53\n" +
                "MDEntryPx: 1.1179\n";
        String dump0 = "" +
                "00000000 c6 53 79 6d 62 6f 6c e6  45 55 52 55 53 44 cb 4e ·Symbol· EURUSD·N\n" +
                "00000010 6f 4d 44 45 6e 74 72 69  65 73 a1 02 cb 4d 44 45 oMDEntri es···MDE\n" +
                "00000020 6e 74 72 79 54 79 70 65  a1 34 c9 4d 44 45 6e 74 ntryType ·4·MDEnt\n" +
                "00000030 72 79 50 78 94 b3 57 cb  4d 44 45 6e 74 72 79 54 ryPx··W· MDEntryT\n" +
                "00000040 79 70 65 a1 35 c9 4d 44  45 6e 74 72 79 50 78 94 ype·5·MD EntryPx·\n" +
                "00000050 ab 57                                            ·W               \n";
        String dump1 = "" +
                "00000000 c6 53 79 6d 62 6f 6c e6  45 55 52 55 53 44 cb 4e ·Symbol· EURUSD·N\n" +
                "00000010 6f 4d 44 45 6e 74 72 69  65 73 a6 02 00 00 00 cb oMDEntri es······\n" +
                "00000020 4d 44 45 6e 74 72 79 54  79 70 65 a1 34 c9 4d 44 MDEntryT ype·4·MD\n" +
                "00000030 45 6e 74 72 79 50 78 91  2e 90 a0 f8 31 e6 f1 3f EntryPx· .···1··?\n" +
                "00000040 cb 4d 44 45 6e 74 72 79  54 79 70 65 a1 35 c9 4d ·MDEntry Type·5·M\n" +
                "00000050 44 45 6e 74 72 79 50 78  91 a5 2c 43 1c eb e2 f1 DEntryPx ··,C····\n" +
                "00000060 3f                                               ?                \n";
        String dump2 = "" +
                "00000000 ba 37 e6 45 55 52 55 53  44 ba 8c 02 a1 02 ba 8d ·7·EURUS D·······\n" +
                "00000010 02 a1 34 ba 8e 02 94 b3  57 ba 8d 02 a1 35 ba 8e ··4····· W····5··\n" +
                "00000020 02 94 ab 57                                      ···W             \n";
        String dump3 = "" +
                "00000000 ba 37 e6 45 55 52 55 53  44 ba 8c 02 a6 02 00 00 ·7·EURUS D·······\n" +
                "00000010 00 ba 8d 02 a1 34 ba 8e  02 91 2e 90 a0 f8 31 e6 ·····4·· ··.···1·\n" +
                "00000020 f1 3f ba 8d 02 a1 35 ba  8e 02 91 a5 2c 43 1c eb ·?····5· ····,C··\n" +
                "00000030 e2 f1 3f                                         ··?              \n";
        String dump4 = "" +
                "00000000 e6 45 55 52 55 53 44 a1  02 a1 34 94 b3 57 a1 35 ·EURUSD· ··4··W·5\n" +
                "00000010 94 ab 57                                         ··W              \n";
        String dump5 = "" +
                "00000000 e6 45 55 52 55 53 44 a6  02 00 00 00 a1 34 91 2e ·EURUSD· ·····4·.\n" +
                "00000010 90 a0 f8 31 e6 f1 3f a1  35 91 a5 2c 43 1c eb e2 ···1··?· 5··,C···\n" +
                "00000020 f1 3f                                            ·?               \n";

        // Return a list of objects arrays containing different test parameter combinations
        return Arrays.asList(
                new Object[]{-1, false, false, false, dump_1},
                new Object[]{0, false, false, false, dump0},
                new Object[]{1, true, false, false, dump1},
                new Object[]{2, false, true, false, dump2},
                new Object[]{3, true, true, false, dump3},
                new Object[]{4, false, false, true, dump4},
                new Object[]{5, true, false, true, dump5}
        );
    }

    // Construct a Wire instance based on testId and other configuration flags
    @NotNull
    private Wire createWire() {
        // Clear any data in the 'bytes' field before constructing the Wire
        bytes.clear();

        // If 'testId' is negative, use TEXT WireType. Otherwise, initialize a binary wire with various configurations.
        @NotNull Wire wire = testId < 0
                ? WireType.TEXT.apply(bytes)
                : new BinaryWire(bytes, fixed, numericField, fieldLess, Integer.MAX_VALUE, "binary");

        return wire;
    }

    // Test method to dump the wire representation of a MarketDataSnapshot instance
    @Test
    public void dump() {
        // Create a Wire instance
        @NotNull Wire wire = createWire();

        // Initialize a MarketDataSnapshot instance with some sample values
        @NotNull MarketDataSnapshot mds = new MarketDataSnapshot("EURUSD", 1.1187, 1.1179);

        // Serialize the market data snapshot to the wire
        mds.writeMarshallable(wire);

        // Print wire's class and configurations to standard output
        System.out.println(wire.getClass().getSimpleName() + ", fixed=" + fixed + ", numericField=" + numericField + ", fieldLess=" + fieldLess);

        // Assert the wire's content, using either its string or hex representation
        if (!wire.isBinary())
            assertEquals(dump, wire.bytes().toString());
        else
            assertEquals(dump, wire.bytes().toHexString());
    }

    // Inner static class representing a snapshot of market data
    static class MarketDataSnapshot implements WriteMarshallable {
        // Fields representing the currency symbol and its opening and closing prices
        String symbol;
        double openingPrice, closingPrice;

        // Constructor to initialize the market data snapshot with provided values
        public MarketDataSnapshot(String symbol, double openingPrice, double closingPrice) {
            this.symbol = symbol;
            this.openingPrice = openingPrice;
            this.closingPrice = closingPrice;
        }

        // Method to serialize this object into a wire format
        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            // Serialize the symbol, entry type, and prices to the provided wire
            wire.write(FIX42.Symbol).text(symbol)
                    .write(FIX42.NoMDEntries).int32(2)
                    .write(FIX42.MDEntryType).uint8('4')
                    .write(FIX42.MDEntryPx).float64(openingPrice)
                    .write(FIX42.MDEntryType).uint8('5')
                    .write(FIX42.MDEntryPx).float64(closingPrice);
        }
    }
}
