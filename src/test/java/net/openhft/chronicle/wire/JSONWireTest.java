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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.IntStream;

import static junit.framework.TestCase.assertNull;
import static net.openhft.chronicle.wire.WireType.JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JSONWireTest extends WireTestCommon {

    // Utility function to test copying from a JSONWire to a binary wire and back to a JSONWire.
    static void testCopyToBinaryAndBack(CharSequence str) {
        // Initialize a JSONWire from the input string
        JSONWire json = new JSONWire(Bytes.from(str));
        HexDumpBytes hexDump = new HexDumpBytes();

        // Initialize a binary wire
        BinaryWire binary = new BinaryWire(hexDump);

        // Initialize another JSONWire for copying back from binary
        JSONWire json2 = new JSONWire(Bytes.allocateElasticOnHeap());
        json2.useTypes(true);

        // Perform the copying operations
        json.copyTo(binary);
//        System.out.println(binary.bytes().toHexString());
        binary.copyTo(json2);

        // Assertions to make sure the copying was successful
        assertEquals(
                str.toString()
                        .replaceAll("\\.0(\\D)", "$1")
                        .replaceAll(" ?\\[ ?", "[")
                        .replaceAll(" ?\\] ?", "]")
                ,
                json2.toString()
                        .replaceAll(" ?\\[ ?", "[")
                        .replaceAll(" ?\\] ?", "]")
        );
        hexDump.releaseLast();
    }

    // Utility function to test copying from a JSONWire to a binary wire and back to a JSONWire.
    static void testCopyToYAMLAndBack(CharSequence str) {
        // Initialize a JSONWire from the input string
        JSONWire json = new JSONWire(Bytes.from(str));

        // Initialize a YAML wire
        YamlWire yaml = new YamlWire(Bytes.allocateElasticOnHeap()).useTextDocuments();

        // Initialize another JSONWire for copying back from binary
        JSONWire json2 = new JSONWire(Bytes.allocateElasticOnHeap());
        json2.useTypes(true);

        // Perform the copying operations
        json.copyTo(yaml);
        System.out.println(yaml);
        yaml.copyTo(json2);

        // Assertions to make sure the copying was successful
        assertEquals(
                str.toString()
                        .replaceAll("\\.0(\\D)", "$1")
                        .replaceAll(" ?\\[ ?", "[")
                        .replaceAll(" ?\\] ?", "]")
                ,
                json2.toString()
                        .replaceAll(" ?\\[ ?", "[")
                        .replaceAll(" ?\\] ?", "]")
        );
    }

    // Utility function to create a JSONWire from a string
    @NotNull
    private JSONWire createWire(@NotNull String json) {
        return new JSONWire(Bytes.from(json));
    }

    // Utility function to create a default JSONWire instance
    @NotNull
    private JSONWire createWire() {
        return new JSONWire(Bytes.allocateElasticDirect()).useTypes(true);
    }

    // Test to verify that opening brackets in the JSON are correctly processed
    @Test
    public void testOpenBracket() {
        @NotNull StringBuilder sb = new StringBuilder();

        @NotNull Wire wire1 = createWire("\"echo\":\"Hello\"\n\"echo2\":\"Hello2\"\n");
        @Nullable String text1 = wire1.readEventName(sb).text();
        assertEquals("echo", sb.toString());
        assertEquals("Hello", text1);
        @Nullable String text2 = wire1.readEventName(sb).text();
        assertEquals("echo2", sb.toString());
        assertEquals("Hello2", text2);

        @NotNull JSONWire wire2 = createWire("{ \"echoB\":\"HelloB\" }\n{ \"echo2B\":\"Hello2B\" }\n");
        @Nullable String textB = wire2.readEventName(sb).text();
        assertEquals("echoB", sb.toString());
        assertEquals("HelloB", textB);

        // finish up one object but keep reading.
        readExpect(wire2, '}');
        wire2.valueIn.stack.reset();

        @Nullable String textB2 = wire2.readEventName(sb).text();
        assertEquals("echo2B", sb.toString());
        assertEquals("Hello2B", textB2);
    }

    // Helper function to ensure the next character in the wire matches the expected character
    private void readExpect(Wire wire2, char expected) {
        wire2.consumePadding();
        assertEquals(expected, (char) wire2.bytes().readByte());
    }

    // Test to check if the JSONWire can process JSON strings without spaces correctly
    @Test
    public void testNoSpaces() {
        @NotNull Wire wire = createWire("\"echo\":\"\"");
        @NotNull VanillaWireParser parser = new VanillaWireParser(soutWireParselet(), VanillaWireParser.SKIP_READABLE_BYTES);
        parser.parseOne(wire);
        assertEquals("", wire.bytes().toString());
    }

    // Returns a wire parselet for printing purposes
    @NotNull
    private WireParselet soutWireParselet() {
        return (s, v) -> System.out.println(s + " - " + v.text());
    }

    // Test for verifying how lists are formatted in JSONWire
    @Test
    public void testListFormatting() {
        @NotNull Wire wire = createWire();

        @NotNull List<Item> items = new ArrayList<>();
        items.add(new Item("item1", 1235666L, 1.1231231));
        items.add(new Item("item2", 2235666L, 1.0987987));
        items.add(new Item("item3", 3235666L, 1.12312));
        items.add(new Item("item4", 4235666L, 1.51231));

        @NotNull WireOut out = wire.writeEventName(() -> "myEvent").list(items, Item.class);

        assertEquals("\"myEvent\":[{\"name\":\"item1\",\"number1\":1235666,\"number2\":1.1231231}," +
                "{\"name\":\"item2\",\"number1\":2235666,\"number2\":1.0987987}," +
                "{\"name\":\"item3\",\"number1\":3235666,\"number2\":1.12312}," +
                "{\"name\":\"item4\",\"number1\":4235666,\"number2\":1.51231}]", out.toString());

        testCopyToBinaryAndBack(out.toString());
        testCopyToYAMLAndBack("{" + out + '}');
        wire.bytes().releaseLast();
    }

    @Test
    public void testNullString() {
        // Create a new wire instance
        @NotNull Wire w = createWire();

        // Create an item with a null name
        @NotNull Item item1 = new Item(null, 1, 2);
        w.write("item").marshallable(item1);

        // Read the item from the wire to another item instance
        @NotNull Item item2 = new Item();
        w.read(() -> "item").marshallable(item2);

        // Assertions to ensure proper reading and writing of the null name
        assertNull(item2.name);
        assertEquals(item1, item2);
        assertEquals(item1.toString(), item2.toString());

        // Release memory occupied by the wire's bytes
        w.bytes().releaseLast();
    }

    @Test
    public void testBytes() {
        // Create a new wire instance
        @NotNull Wire w = createWire();

        // Write some values into the wire
        Bytes<?> bs = Bytes.from("blablabla");
        w.write("a").int64(123);
        w.write("somebytes").text(bs);

        // Create another wire instance from the bytes of the first wire
        Wire w2 = WireType.JSON.apply(w.bytes());
        assertEquals("\"a\":123,\"somebytes\":\"blablabla\"", w2.toString());

        // Read the data back from the wire into a ByteBuffer
        Bytes<java.nio.ByteBuffer> bb = Bytes.elasticByteBuffer();
        assertEquals(123, w2.read("a").int64());

        w2.read("somebytes").text(bb);
        assertEquals(bs, bb);

        // Release occupied memory
        w2.bytes().releaseLast();
        bb.releaseLast();
    }

    @Test
    public void testFloatFromJson() {
        // Create a new FooEvent and set its foo value
        FooEvent foo = new FooEvent();
        foo.foo = 0.1f;

        // Convert the FooEvent to a JSON string
        @NotNull CharSequence str = WireType.JSON.asString(foo);
        assertEquals("{\"foo\":0.1}", str);
        // 0.1 when cast to a double is 0.10000000149011612. We used to throw an exception here because of this difference
        FooEvent foo2 = WireType.JSON.fromString(FooEvent.class, str);
        assertEquals(foo, foo2);
    }

    @Test
    public void testMarshallableWithTwoLists() {
        // Create a new wire instance
        @NotNull Wire wire = createWire();

        // Write a TwoLists object with two lists into the wire
        @NotNull TwoLists lists1 = new TwoLists(null, 5, 5);
        wire.writeEventName("two_lists").marshallable(lists1);

        // Read the TwoLists object back from the wire
        @NotNull TwoLists lists2 = new TwoLists();

        @NotNull final StringBuilder sb = new StringBuilder();
        @NotNull ValueIn valueIn = wire.readEventName(sb);

        valueIn.marshallable(lists2);

        // fails due to a trailing space if we don't call toString.
        // assertEquals(lists1, lists2);
        // assertEquals(lists1.toString(), lists2.toString());
        try {
            assertEquals("!net.openhft.chronicle.wire.JSONWireTest$TwoLists {\n" +
                    "  name: !!null \"\",\n" +
                    "  list1: [\n" +
                    "    { name: !!null \"\", number1: 0, number2: 0.0 },\n" +
                    "    { name: !!null \"\", number1: 1, number2: 10.0 },\n" +
                    "    { name: !!null \"\", number1: 2, number2: 20.0 },\n" +
                    "    { name: !!null \"\", number1: 3, number2: 30.0 },\n" +
                    "    { name: !!null \"\", number1: 4, number2: 40.0 }\n" +
                    "  ],\n" +
                    "  list2: [\n" +
                    "    { name: !!null \"\", number1: 0, number2: 0.0 },\n" +
                    "    { name: !!null \"\", number1: 1, number2: 10.0 },\n" +
                    "    { name: !!null \"\", number1: 2, number2: 20.0 },\n" +
                    "    { name: !!null \"\", number1: 3, number2: 30.0 },\n" +
                    "    { name: !!null \"\", number1: 4, number2: 40.0 }\n" +
                    "  ]\n" +
                    "}\n", lists1.toString());
            final String str = JSON.asString(lists1);
            testCopyToBinaryAndBack(str);
//            testCopyToYAMLAndBack(str);
        } finally {
            // Release occupied memory
            wire.bytes().releaseLast();
        }
    }

    @Test
    public void testMapOfNamedKeys() {
        MapHolder mh = new MapHolder(); // Create a new MapHolder object
        Map<RetentionPolicy, Double> map = Collections.singletonMap(RetentionPolicy.CLASS, 0.1); // Define a map with a single entry
        mh.map = map; // Assign the created map to the map attribute of MapHolder
        doTestMapOfNamedKeys(mh); // Test with the simple map
        mh.map = new TreeMap<>(map); // Convert map to TreeMap
        doTestMapOfNamedKeys(mh); // Test with TreeMap
        mh.map = new HashMap<>(map); // Convert map to HashMap
        doTestMapOfNamedKeys(mh); // Test with HashMap
        mh.map = new LinkedHashMap<>(map); // Convert map to LinkedHashMap
        doTestMapOfNamedKeys(mh); // Test with LinkedHashMap
    }

    @Test
    public void testMapOfNamedKeys2() {
        String str = "{\"map\":{\"CLASS\":0.1}}";
        testCopyToBinaryAndBack(str); // Test binary and back with the specified string
        testCopyToYAMLAndBack(str);
    }

    private void doTestMapOfNamedKeys(MapHolder mh) {
        // Convert MapHolder to JSON string and assert its format
        assertEquals("{\"map\":{\"CLASS\":0.1}}", JSON.asString(mh));
    }

    @Test
    public void testDate() {
        Dates dates = new Dates(); // Create a new Dates object
        dates.date = LocalDate.of(2021, 5, 28); // Set specific LocalDate
        dates.dateTime = LocalDateTime.of(2020, 4, 26, 6, 35, 11); // Set specific LocalDateTime
        dates.zdateTime = ZonedDateTime.of(dates.dateTime, ZoneId.of("UTC")); // Set ZonedDateTime based on LocalDateTime
        @NotNull CharSequence str = WireType.JSON.asString(dates); // Convert Dates object to JSON string
        String expected = "{\"date\":\"2021-05-28\",\"dateTime\":\"2020-04-26T06:35:11\",\"zdateTime\":\"2020-04-26T06:35:11Z[UTC]\"}";
        assertEquals(expected, str); // Assert that the JSON string matches the expected format

        JSONWire jw = new JSONWire(Bytes.allocateElasticOnHeap()); // Create a new JSONWire with allocated bytes
        jw.trimFirstCurly(false);
        jw.getValueOut().typedMarshallable(dates); // Write the dates to the wire
        assertEquals(expected, jw.toString()); // Assert that the JSONWire output matches the expected format

        testCopyToBinaryAndBack(str); // Test binary and back with the provided string
        testCopyToYAMLAndBack(str);
    }

    @Test
    public void commaIsNotInAValue() {
        String text = "[1,2,3]"; // Define a string representation of a list
        Wire wire = createWire(); // Create a new wire
        wire.bytes().append(text); // Append the list string to the wire
        final Object list = wire.getValueIn().object(); // Extract the list from the wire
        assertEquals("[1, 2, 3]", "" + list); // Assert that the extracted list matches the expected format

        String text2 = "[ 1, 2, 3 ]"; // Define another string representation with spaces
        wire.bytes().clear().append(text2); // Clear the wire and append the new string
        final Object list2 = wire.getValueIn().object(); // Extract the list from the wire
        assertEquals("[1, 2, 3]", "" + list2); // Assert that the extracted list matches the expected format
    }

    @Test
    public void testArrayInDictionary() {
        String text = "[320 , {\"as\":[1 ,\n2\n, 3]}]"; // Define a string with a list that contains an integer and a dictionary
        final JSONWire jsonWire = new JSONWire(Bytes.from(text)); // Create a new JSONWire with the given text
        final Object list = jsonWire.getValueIn().object(); // Extract the object from the JSONWire
        assertEquals("[320, {as=[1, 2, 3]}]", "" + list); // Assert that the extracted object matches the expected format
    }

    @Test
    public void testDateNull() {
        Dates dates = new Dates(); // Create a new Dates object with presumably null fields
        @NotNull CharSequence str = WireType.JSON.asString(dates); // Convert the Dates object to a JSON string
        // Assert that all date fields in the JSON string are null
        assertEquals("{\"date\":null,\"dateTime\":null,\"zdateTime\":null}", str);
        testCopyToBinaryAndBack(str); // Test binary conversion and back with the JSON string
        testCopyToYAMLAndBack(str);
    }

    @Test
    public void testArrayInDictionary2() {
        // Define a complex JSON string containing nested arrays and dictionaries
        String str = "[320,{\"as\":[[\"32905.50000\",\"1.60291699\",\"1625822573.857656\"],[\"32905.60000\",\"0.10415889\",\"1625822573.194909\"]],\"bs\":[[\"32893.60000\",\"0.15042948\",\"1625822574.220475\"]]},\"book-10\"]";
        final JSONWire jsonWire = new JSONWire(Bytes.from(str)); // Create a JSONWire object from the str
        final Object list = jsonWire.getValueIn().object(); // Extract the content of the wire
        // Assert that the extracted content matches the expected format
        assertEquals("[320, {as=[[32905.50000, 1.60291699, 1625822573.857656], [32905.60000, 0.10415889, 1625822573.194909]], bs=[[32893.60000, 0.15042948, 1625822574.220475]]}, book-10]", "" + list);
        testCopyToBinaryAndBack(str); // Test binary conversion and back with the provided JSON string
        testCopyToYAMLAndBack('{'+str+'}');
    }

    @Test
    public void testArrayDelimiterNoSpace() {
        // A complex JSON string causing some parsing issues
        String str = "[320,{\"as\":[[\"32905.50000\",\"1.60291699\",\"1625822573.857656\"],[\"32905.60000\",\"0.10415889\",\"1625822573.194909\"]],\"bs\":[[\"32893.60000\",\"0.15042948\",\"1625822574.220475\"]]},\"book-10\"]";

        // A simple version of JSON str for testing purposes (commented out)
//        String str = "[1,{\"a\":[2,3]}]";

        // A different simple JSON string that seems to work
//        String str = "[1,2,3,\"c\"]";

        final Bytes<ByteBuffer> byteBufferBytes = Bytes.elasticByteBuffer(); // Create an elastic byte buffer
        byteBufferBytes.append(str); // Append the JSON string to the byte buffer

        final JSONWire jsonWire = new JSONWire(byteBufferBytes); // Create a JSONWire object using the byte buffer

        final List<Object> list = jsonWire.getValueIn().list(Object.class); // Extract the content of the wire into a list
        assertNotNull(list); // Assert that the extracted list is not null
        testCopyToBinaryAndBack(str); // Test binary conversion and back with the JSON string
        testCopyToYAMLAndBack('{'+str+'}');
        byteBufferBytes.releaseLast(); // Release the last buffer to free up resources
    }

    @Test
    public void testQuotedFieldsEmptySequence() {
        // Create a JSON string with different field types
        final Bytes<byte[]> data = Bytes.allocateElasticOnHeap();
        data.append("{\n" +
                "  \"field1\": 1234 , " +
                "  \"field2\": 456\n,\n" +
                "  \"field3\": [ ] ,\n" +
                "  \"field4\": [\n" +
                "    \"abc\" ,\n" +
                "    \"xyz\"\n" +
                "  ]\n" +
                "}");

        final JSONWire wire = new JSONWire(data); // Create a JSONWire object from the data
        final SimpleTwoLists f = new SimpleTwoLists(); // Instantiate a new SimpleTwoLists object
        wire.getValueIn().object(f, SimpleTwoLists.class); // Read the content of the wire into the object

        // Assert that the resulting JSON string from the object matches the expected format
        assertEquals("{\"field1\":1234,\"field2\":456,\"field3\":[ ],\"field4\":[\"abc\",\"xyz\" ]}", JSON.asString(f));
    }

    // A static class to demonstrate a holder of maps with different types of numeric keys and string values
    static class MapWithIntegerKeysHolder extends SelfDescribingMarshallable {
        Map<Integer, String> intMap = new LinkedHashMap<>(); // A map with integer keys
        Map<Long, String> longMap = new LinkedHashMap<>();   // A map with long keys
        Map<Double, String> doubleMap = new LinkedHashMap<>(); // A map with double keys
    }

    @Test
    public void nestedMapWithIntegerKeys() {
        MapWithIntegerKeysHolder mh = new MapWithIntegerKeysHolder(); // Create an instance of MapWithIntegerKeysHolder
        // Populate the maps inside the MapWithIntegerKeysHolder object with test data
        mh.intMap.put(1111, "ones");
        mh.intMap.put(2222, "twos");
        mh.longMap.put(888888888888L, "eights");
        mh.longMap.put(999999999999L, "nines");
        mh.doubleMap.put(1.28, "number");
        mh.doubleMap.put(2.56, "number");
        final String str = JSON.asString(mh); // Convert the populated object to its JSON string representation
        // Assert the generated JSON string matches the expected JSON string
        assertEquals("" +
                        "{\"intMap\":{\"1111\":\"ones\",\"2222\":\"twos\"},\"longMap\":{\"888888888888\":\"eights\",\"999999999999\":\"nines\"},\"doubleMap\":{\"1.28\":\"number\",\"2.56\":\"number\"}}",
                str);
        // Convert the JSON string back to a new instance of MapWithIntegerKeysHolder
        MapWithIntegerKeysHolder mh2 = JSON.fromString(MapWithIntegerKeysHolder.class, str);
        assertEquals(mh, mh2); // Assert that the original and reconverted objects are the same
        testCopyToBinaryAndBack(str); // Test binary conversion and back with the JSON string
        testCopyToYAMLAndBack(str);
    }

    @Test
    public void testWritingLayout() {
        final Bytes<byte[]> bytes = Bytes.allocateElasticOnHeap(1024); // Create an elastic byte buffer
        final JSONWire wire = new JSONWire(bytes, true); // Create a JSONWire object

        final Value foo = new Value(); // Create an instance of the Value class

        wire.getValueOut().marshallable(foo); // Write the Value object to the wire

        // Assert the content of the wire (in string format) matches the expected JSON string
        assertEquals("{\"a\":{\"b\":\"c\"}}", bytes.toString());
    }

    @Test
    public void escapeUnicodeValues() {
        Map<Object, Object> map = new HashMap<>(); // Create a new HashMap
        // Stream over a range of Unicode code points
        IntStream.rangeClosed(0x0000, 0x0020)
                .forEach(code -> {
                    map.put("key", (char) code); // Add an entry to the map with the character representation of the current code point

                    final String text = JSON.asString(map); // Convert the map to a JSON string
                    String val;
                    // Map the current code point to its escaped string representation
                    switch (code) {
                        case '\b':
                            val = "\\b";
                            break;
                        case '\f':
                            val = "\\f";
                            break;
                        case '\n':
                            val = "\\n";
                            break;
                        case '\r':
                            val = "\\r";
                            break;
                        case '\t':
                            val = "\\t";
                            break;
                        case ' ':
                            val = " ";
                            break;
                        default:
                            val = String.format("\\u%04X", code);
                            break;
                    }
                    // Assert the generated JSON string matches the expected format
                    assertEquals("{\"key\":\"" + val + "\"}", text);
                });
    }

    @Test
    public void copyTypedDataToBinaryAndBack() {
        ignoreException("Unable to copy object safely, message will not be repeated");
        String str = "{\"a\":{\"@Mapped\":{\"b\":\"c\",\"d\":123.4}},\"e\":{\"@Scalar\":\"Value\"},\"f\":{\"@Mapped2\":{\"b\":\"c\"}},\"g\":{\"@Scalar2\":12345.6}}";
        testCopyToBinaryAndBack(str);
        testCopyToYAMLAndBack(str);
    }

    // A class to represent a nested structure for testing JSON serialization
    private static class Value extends SelfDescribingMarshallable {
        final Inner a = new Inner(); // Inner object

        // Nested class to define an inner structure of the Value class
        private static class Inner extends SelfDescribingMarshallable {
            String b = "c"; // A single field with a default value
        }
    }

    // Another simple class extending a hypothetical abstract class "AbstractEventCfg"
    private static class FooEvent extends AbstractEventCfg<FooEvent> {
        float foo; // A single float field
    }

    // A simple class representing an item with some properties
    private static class Item extends SelfDescribingMarshallable {
        String name;       // Name of the item
        long number1;      // Some numerical value
        double number2;    // Another numerical value

        // Default constructor
        Item() {
        }

        // Parametrized constructor to initialize the item with provided values
        Item(String name, long number1, double number2) {
            this.name = name;
            this.number1 = number1;
            this.number2 = number2;
        }
    }

    // Class representing two lists of items
    private static class TwoLists implements Marshallable {
        @Nullable
        String name;          // Optional name for the list holder
        List<Item> list1;     // First list of items
        List<Item> list2;     // Second list of items

        // Default constructor
        TwoLists() {
        }

        // Parametrized constructor to initialize the object with provided values and generate lists based on those values
        TwoLists(@Nullable String name, long number1, double number2) {
            this.name = name;
            this.list1 = new ArrayList<>();
            // Generate 'number1' items for the first list
            for (int i = 0; i < number1; i++) {
                list1.add(new Item(name, i, i * 10));
            }
            this.list2 = new ArrayList<>();
            // Generate 'number2' items for the second list
            for (int i = 0; i < number2; i++) {
                list2.add(new Item(name, i, i * 10));
            }
        }

        // Override toString method to use Marshallable's utility function for conversion to string
        @Override
        public String toString() {
            return Marshallable.$toString(this);
        }

        // Method to deserialize the object's state from the wire format
        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            name = wire.read(() -> "name").text();  // Deserialize 'name' field
            list1 = wire.read(() -> "list1").list(Item.class);  // Deserialize 'list1'
            list2 = wire.read(() -> "list2").list(Item.class);  // Deserialize 'list2'
        }

        // Method to serialize the object's state to the wire format
        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            wire.write(() -> "name").text(name);  // Serialize 'name' field
            wire.write(() -> "list1").list(list1, Item.class);  // Serialize 'list1'
            wire.write(() -> "list2").list(list2, Item.class);  // Serialize 'list2'
        }
    }

    // Class to hold a map of retention policies to their double values
    static class MapHolder extends SelfDescribingMarshallable {

        Map<RetentionPolicy, Double> map;
    }

    // Class to encapsulate date-related fields including local, local with time, and zoned date-times
    private static class Dates extends SelfDescribingMarshallable {
        LocalDate date;           // Representing a date without time-zone
        LocalDateTime dateTime;   // Representing a date-time without a time-zone
        ZonedDateTime zdateTime;  // Representing a date-time with a time-zone

        Dates() {
        }
    }

    // Class to represent an entity with two fields and two lists of strings
    private static final class SimpleTwoLists implements Marshallable {
        int field1; // Integer field 1
        int field2; // Integer field 2
        final List<String> field3 = new ArrayList<>(); // List of strings for field3
        final List<String> field4 = new ArrayList<>(); // List of strings for field4

        // Method to read marshallable data from the wire input
        @Override
        public void readMarshallable(@NotNull final WireIn wire) throws IORuntimeException {
            wire.read(() -> "field1").int32(this, (self, n) -> self.field1 = n); // Reading integer value for field1
            wire.read(() -> "field2").int32(this, (self, n) -> self.field2 = n); // Reading integer value for field2
            wire.read(() -> "field3").sequence(this, field3, SimpleTwoLists::readList); // Reading sequence for field3
            wire.read(() -> "field4").sequence(this, field4, SimpleTwoLists::readList); // Reading sequence for field4
        }

        // Helper method to read and add string values from the reader to a list
        private static void readList(SimpleTwoLists record, List<String> data, ValueIn reader) {
            while (reader.hasNextSequenceItem()) { // Looping through sequence items
                data.add(reader.text()); // Adding each text item to the list
            }
        }
    }

    @Test
    public void typeLiteral1() {
        String expected = "{\"@net.openhft.chronicle.wire.JSONWireTest$DtoWithClassReference\":{\"implClass\":{\"@type\":\"net.openhft.chronicle.wire.JSONWireTest\"},\"bool\":false}}";
        Object o = WireType.JSON_ONLY.fromString(expected);
        String json = WireType.JSON_ONLY.asString(o);
        Assert.assertEquals(expected, json);
    }

    @Test
    public void typeLiteralTest2() {
        DtoWithClassReference dtoWithClassReference = new DtoWithClassReference();
        dtoWithClassReference.implClass = this.getClass();
        String json = WireType.JSON_ONLY.asString(dtoWithClassReference);
        assertEquals("{\"@net.openhft.chronicle.wire.JSONWireTest$DtoWithClassReference\"" +
                        ":{\"implClass\":{\"@type\":\"net.openhft.chronicle.wire.JSONWireTest\"},\"bool\":false}}",
                json);
        assertEquals(dtoWithClassReference, WireType.JSON_ONLY.fromString(json));
    }

    private static class DtoWithClassReference extends SelfDescribingMarshallable {
        private Class<?> implClass;
        private boolean bool;
    }

}
