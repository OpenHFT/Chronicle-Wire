/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;

public class WireMarshallerTest extends WireTestCommon {

    @Test
    public void usesBinary() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        // Add an alias for the WMTwoFields class.
        ClassAliasPool.CLASS_ALIASES.addAlias(WMTwoFields.class);

        // Define a string representation of the WMTwoFields object in YAML format.
        String text = "!WMTwoFields {\n" +
                "  id: shelf.script.door,\n" +
                "  ts: 2019-11-17T12:56:42.108971\n" +
                "}\n";

        // Parse the string representation to get the WMTwoFields object.
        WMTwoFields tf = Marshallable.fromString(text);

        // Check the parsed object is not null.
        assert tf != null;

        // Assert that the string representation of the parsed object matches the original text.
        assertEquals(text, tf.toString());

        // Create a HexDumpBytes object for examining binary content in hexadecimal format.
        HexDumpBytes bytes = new HexDumpBytes();

        // Initialize the BinaryWire for serialization.
        Wire w = new BinaryWire(bytes);

        // Serialize the WMTwoFields object to binary using Wire.
        w.write("").object(WMTwoFields.class, tf);

        // Deserialize the WMTwoFields object from binary using Wire.
        WMTwoFields tf2 = w.read().object(WMTwoFields.class);

        // Assert that the string representation of the deserialized object matches the original text.
        assertEquals(text, tf2.toString());

        // Assert that the serialized binary content matches the expected hexadecimal format.
        assertEquals("" +
                "c0                                              # :\n" +
                "82 14 00 00 00                                  # WMTwoFields\n" +
                "   c2 69 64                                        # id:\n" +
                "   a6 d2 02 96 49                                  # 1234567890\n" +
                "   c2 74 73                                        # ts:\n" +
                "   a7 2b 20 d2 5c 8a 97 05 00                      # 1573995402108971\n", bytes.toHexString());

        // Release the resources used by the HexDumpBytes object.
        bytes.releaseLast();
    }

    @Test
    public void fieldOrderAppliesPerClassInInheritanceHierarchy() {
        Map<String, Field> fields = new LinkedHashMap<>();
        WireMarshaller.getAllField(Trade.class, fields);

        assertEquals("baseId,createdTime,symbol,price,quantity",
                String.join(",", fields.keySet()));
    }

    @Test
    public void fieldOrderControlsSelfDescribingMarshallableOutput() {
        Trade trade = new Trade();
        trade.baseId = 17;
        trade.createdTime = 123456789L;
        trade.symbol = "EURUSD";
        trade.price = 1.2345;
        trade.quantity = 1_000;

        assertEquals("!net.openhft.chronicle.wire.WireMarshallerTest$Trade {\n" +
                "  baseId: 17,\n" +
                "  createdTime: 123456789,\n" +
                "  symbol: EURUSD,\n" +
                "  price: 1.2345,\n" +
                "  quantity: 1000\n" +
                "}\n", trade.toString());
    }

    @Test
    public void fieldOrderRejectsIncompleteFieldList() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> WireMarshaller.of(IncompleteFieldOrder.class));

        assertEquals(WireMarshallerTest.class.getName()
                        + "$IncompleteFieldOrder @FieldOrder is missing marshallable fields: [second]",
                e.getMessage());
    }

    @Test
    public void fieldOrderRejectsDuplicateFieldNames() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> WireMarshaller.of(DuplicateFieldOrder.class));

        assertEquals(WireMarshallerTest.class.getName()
                        + "$DuplicateFieldOrder @FieldOrder contains duplicate field: first",
                e.getMessage());
    }

    @Test
    public void fieldOrderRejectsBlankFieldNames() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> WireMarshaller.of(BlankFieldOrder.class));

        assertEquals(WireMarshallerTest.class.getName()
                        + "$BlankFieldOrder @FieldOrder contains blank field name at index 1",
                e.getMessage());
    }

    @Test
    public void fieldOrderTrimsWhitespaceAroundFieldNames() {
        Map<String, Field> fields = new LinkedHashMap<>();
        WireMarshaller.getAllField(WhitespaceFieldOrder.class, fields);

        assertEquals("second,first", String.join(",", fields.keySet()));
    }

    @Test
    public void fieldOrderRejectsDuplicateFieldNamesAfterTrimming() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> WireMarshaller.of(TrimmedDuplicateFieldOrder.class));

        assertEquals(WireMarshallerTest.class.getName()
                        + "$TrimmedDuplicateFieldOrder @FieldOrder contains duplicate field: first",
                e.getMessage());
    }

    @Test
    public void fieldOrderRejectsTransientField() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> WireMarshaller.of(UnknownFieldOrder.class));

        assertEquals(WireMarshallerTest.class.getName()
                        + "$UnknownFieldOrder @FieldOrder references non-marshallable field: ignored (static, transient, or otherwise excluded)",
                e.getMessage());
    }

    @Test
    public void fieldOrderRejectsInheritedFieldName() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> WireMarshaller.of(InheritedFieldOrder.class));

        assertEquals(WireMarshallerTest.class.getName()
                        + "$InheritedFieldOrder @FieldOrder references unknown field: parentField; declared marshallable fields are [childField]",
                e.getMessage());
    }

    @Test
    public void fieldlessBinaryWithMismatchedFieldOrderSwapsValues() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            WireType.FIELDLESS_BINARY.apply(bytes).getValueOut().object(new SenderXYZ(1, 2, 3));
            ReceiverZYX r = WireType.FIELDLESS_BINARY.apply(bytes).getValueIn().object(ReceiverZYX.class);
            assertEquals(3, r.x);
            assertEquals(2, r.y);
            assertEquals(1, r.z);
        } finally {
            bytes.releaseLast();
        }
    }

    @FieldOrder({"baseId", "createdTime"})
    static class Base extends SelfDescribingMarshallable {
        long createdTime;
        long baseId;
    }

    @FieldOrder({"symbol", "price", "quantity"})
    static class Trade extends Base {
        double price;
        String symbol;
        long quantity;
    }

    @FieldOrder({"first"})
    static class IncompleteFieldOrder extends SelfDescribingMarshallable {
        int first;
        int second;
    }

    @FieldOrder({"first", "first"})
    static class DuplicateFieldOrder extends SelfDescribingMarshallable {
        int first;
        int second;
    }

    @FieldOrder({"first", " "})
    static class BlankFieldOrder extends SelfDescribingMarshallable {
        int first;
        int second;
    }

    @FieldOrder({" second ", " first "})
    static class WhitespaceFieldOrder extends SelfDescribingMarshallable {
        int first;
        int second;
    }

    @FieldOrder({"first", " first "})
    static class TrimmedDuplicateFieldOrder extends SelfDescribingMarshallable {
        int first;
        int second;
    }

    @FieldOrder({"first", "ignored"})
    static class UnknownFieldOrder extends SelfDescribingMarshallable {
        int first;
        int second;
        transient int ignored;
    }

    static class InheritedFieldOrderParent extends SelfDescribingMarshallable {
        int parentField;
    }

    @FieldOrder({"parentField", "childField"})
    static class InheritedFieldOrder extends InheritedFieldOrderParent {
        int childField;
    }

    @FieldOrder({"x", "y", "z"})
    static class SenderXYZ extends SelfDescribingMarshallable {
        int x, y, z;
        @SuppressWarnings("unused") SenderXYZ() {}
        SenderXYZ(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
    }

    @FieldOrder({"z", "y", "x"})
    static class ReceiverZYX extends SelfDescribingMarshallable {
        int x, y, z;
    }
}
