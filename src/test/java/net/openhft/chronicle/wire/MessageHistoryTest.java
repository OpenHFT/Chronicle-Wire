/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static net.openhft.chronicle.bytes.MethodReader.MESSAGE_HISTORY_METHOD_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings({"deprecation", "removal"})
class MessageHistoryTest extends WireTestCommon {
    private static final String EXPECTED_COPYABLE_YAML = "history: {\n" +
            "  sources: [\n" +
            "    1,\n" +
            "    0x2\n" +
            "  ],\n" +
            "  timings: [\n" +
            "    11111111,\n" +
            "    22222222,\n" +
            "    120962203520100\n" +
            "  ]\n" +
            "}\n";

    // Test to check if clearing and retrieving the MessageHistory works correctly.
    @Test
    @DisplayName("MessageHistory get and clear should create a fresh instance")
    void checkHistoryGetClear() {
        // Retrieve the current message history.
        MessageHistory mg = MessageHistory.get();
        assertNotNull(mg, "MessageHistory.get should return a history instance");

        // Reset the message history.
        MessageHistory.clear();

        // Retrieve a new instance of message history.
        MessageHistory mg2 = MessageHistory.get();
        assertNotNull(mg2, "MessageHistory.get should return a new instance after clear");

        // Ensure that the two message histories are not the same instance.
        assertNotSame(mg, mg2, "MessageHistory.clear should replace the history instance");
    }

    // Test the deep copy functionality of the VanillaMessageHistory.
    @Test
    @DisplayName("VanillaMessageHistory deep copy preserves sources and timings")
    void checkDeepCopy() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for deep copy history test");

        // Initialize a new history and add sources and timings.
        VanillaMessageHistory history = new VanillaMessageHistory();
        initExampleMessageHistory(history);
        VanillaMessageHistory history2 = history.deepCopy();

        // Check if the original and copied histories are equal.
        assertEquals(history, history2, "deepCopy should preserve sources and timings");
    }

    // Test to check if an exception is thrown when history exceeds maximum size.
    @Test
    @DisplayName("MessageHistory should enforce maximum size limit")
    void checkHistoryMaxSizeException() {
        VanillaMessageHistory container1 = new VanillaMessageHistory();
        container1.useBytesMarshallable(!OS.isMacOSX());
        container1.addSourceDetails(true);
        VanillaMessageHistory container2 = new VanillaMessageHistory();
        container2.useBytesMarshallable(!OS.isMacOSX());
        container2.addSourceDetails(true);

        // Copy data between containers until reaching the message history length limit.
        for (int i = 0; i < VanillaMessageHistory.MESSAGE_HISTORY_LENGTH / 2; i++) {
            Wires.copyTo(container1, container2);
            Wires.copyTo(container2, container1);
        }

        // this is the limit, one more timing should fail
        assertEquals(256, container1.timings(), "timings should reach maximum history length");
        // Attempt to copy again and expect an exception.
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> Wires.copyTo(container1, container2),
                "copyTo beyond max size should throw");
        if (thrown instanceof ArithmeticException) {
            assertTrue(thrown.getMessage().contains("257 out of range"),
                    "exception message should include out of range count");
        }
    }

    // Test the serialization of bytes in the VanillaMessageHistory.
    @Test
    @DisplayName("MessageHistory should serialise and deserialise byte streams")
    void checkSerialiseBytes() {

        // Initialize a new history and add sources and timings.
        VanillaMessageHistory history = new SetTimeMessageHistory();
        initExampleMessageHistory(history);
        BinaryWire bw = new BinaryWire(Bytes.elasticHeapByteBuffer());
        history.writeMarshallable(bw);

        // Deserialize the bytes to a new history instance.
        VanillaMessageHistory history2 = new SetTimeMessageHistory();
        history2.historyWallClock(false);
        history2.readMarshallable(bw);

        // Ensure the deserialized history matches the expected format.
        assertEquals("VanillaMessageHistory { " +
                "sources: [1=0xff,2=0xfff], " +
                "timings: [1000000000000000000,1000000000000010000,120962203520100,120962203520100], " +
                "addSourceDetails=true }", history2.toString(),
                "Deserialised history should match expected string without source context");

        // Adjust the read position and add source details.
        bw.bytes().readPosition(0);
        history2.addSourceDetails(true);

        // Create a source context for deserialization.
        SourceContext sc = new SourceContext() {
            @Override
            public int sourceId() {
                return 3;
            }

            @Override
            public long index() throws IORuntimeException {
                return 0xffff;
            }
        };
        bw.parent(sc);
        history2.readMarshallable(bw);

        // Ensure the deserialized history with source details matches the expected format.
        assertEquals("VanillaMessageHistory { " +
                "sources: [1=0xff,2=0xfff,3=0xffff], " +
                "timings: [1000000000000000000,1000000000000010000,120962203520100,120962203520100], " +
                "addSourceDetails=true }", history2.toString(),
                "Deserialised history should include source context");
    }

    // Test the toString() representation of the VanillaMessageHistory.
    @Test
    @DisplayName("MessageHistory toString renders expected hex and text")
    void checkToString() {
        {
            VanillaMessageHistory history = new SetTimeMessageHistory();
            history.historyWallClock(true);
            history.useBytesMarshallable(false);
            history.addSourceDetails(true);
            history.historyWallClock(true);
            initExampleMessageHistory(history);
            assertEquals(2, history.sources(), "history should contain two sources");
            assertEquals(2, history.timings(), "history should contain two timings");

            // Serialize the message history into hex dump bytes.
            BinaryWire bw = new BinaryWire(new HexDumpBytes());
            bw.writeEventName(MethodReader.HISTORY).marshallable(history);
            assertEquals("b9 07 68 69 73 74 6f 72 79                      # history: (event)\n" +
                            "81 4b 00                                        # SetTimeMessageHistory\n" +
                            "c7 73 6f 75 72 63 65 73                         # sources:\n" +
                            "82 16 00 00 00                                  # sequence\n" +
                            "                                                # source id & index\n" +
                            "a1 01 af ff 00 00 00 00 00 00 00                # 1\n" +
                            "                                                # source id & index\n" +
                            "a1 02 af ff 0f 00 00 00 00 00 00                # 2\n" +
                            "c7 74 69 6d 69 6e 67 73                         # timings:\n" +
                            "82 1b 00 00 00                                  # sequence\n" +
                            "                                                # timing in nanos\n" +
                            "a7 00 00 64 a7 b3 b6 e0 0d                      # 1000000000000000000\n" +
                            "                                                # timing in nanos\n" +
                            "a7 10 27 64 a7 b3 b6 e0 0d                      # 1000000000000010000\n" +
                            "a7 64 0c 2c b5 03 6e 00 00                      # 120962203520100\n",
                    bw.bytes().toHexString(),
                    "Hex dump should match expected history encoding");

            // Release the bytes from the wire.
            bw.bytes().releaseLast();

            assertEquals("VanillaMessageHistory { sources: [1=0xff,2=0xfff], timings: [ 2001-09-09T01:46:40, 2001-09-09T01:46:40.00001 ], addSourceDetails=true }",
                    history.toString(), "toString should include sources and timings in wall clock mode");
            assertEquals(2, history.sources(), "history should still contain two sources");
            assertEquals(2, history.timings(), "history should still contain two timings");

            BinaryWire bw2 = new BinaryWire(new HexDumpBytes());
            history.useBytesMarshallable(true);
            bw2.writeEventName(MethodReader.HISTORY).marshallable(history);
            assertEquals("b9 07 68 69 73 74 6f 72 79                      # history: (event)\n" +
                            "81 33 00 86                                     # SetTimeMessageHistory\n" +
                            "02 01 00 00 00 02 00 00 00 ff 00 00 00 00 00 00 # sources\n" +
                            "00 ff 0f 00 00 00 00 00 00 03 00 00 64 a7 b3 b6 # timings\n" +
                            "e0 0d 10 27 64 a7 b3 b6 e0 0d 64 0c 2c b5 03 6e\n" +
                            "00 00\n",
                    bw2.bytes().toHexString(),
                    "Hex dump should match bytes-marshallable encoding");
            bw2.bytes().releaseLast();

            // check direct and on heap memory serialize the same.
            Wire wire1 = new BinaryWire(Bytes.allocateElasticOnHeap());
            Wire wire2 = new BinaryWire(Bytes.allocateElasticDirect());
            history.writeMarshallable(wire1);
            history.writeMarshallable(wire2);
            String hexString1 = wire1.bytes().toHexString();
            String hexString2 = wire2.bytes().toHexString();
            assertEquals(hexString1, hexString2, "Direct and heap serialisation should match");
            VanillaMessageHistory mh1 = new SetTimeMessageHistory();
            mh1.historyWallClock(true);
            mh1.addSourceDetails(false);
            mh1.readMarshallable(wire1);
            assertTrue(mh1.toString().startsWith("VanillaMessageHistory { sources: [1=0xff,2=0xfff], timings: [ 2001-09-09T01:46:40, 2001-09-09T01:46:40.00001,"),
                    "Heap history should include expected sources and timings prefix");
            VanillaMessageHistory mh2 = new SetTimeMessageHistory();
            mh2.historyWallClock(true);
            mh2.addSourceDetails(false);
            mh2.readMarshallable(wire2);
            assertTrue(mh2.toString().startsWith("VanillaMessageHistory { sources: [1=0xff,2=0xfff], timings: [ 2001-09-09T01:46:40, 2001-09-09T01:46:40.00001,"),
                    "Direct history should include expected sources and timings prefix");
        }
    }

    // Tests the readMarshallable functionality using different configurations.
    @Test
    @DisplayName("MessageHistory readMarshallable matches expected wire output")
    void testReadMarshallable() {
        {
            SetTimeMessageHistory vmh = new SetTimeMessageHistory();
            vmh.historyWallClock(true);
            vmh.addSource(1, 2);
            vmh.addTiming(1111);
            vmh.addTiming(2222);

            // Serialize the message history to hex dump bytes.
            HexDumpBytes bytes = new HexDumpBytes();
            Wire wire = new BinaryWire(bytes);
            vmh.useBytesMarshallable(false);
            wire.writeEventName(MethodReader.HISTORY).object(SetTimeMessageHistory.class, vmh);

            // Change the nanoTime and serialize with a different configuration.
            vmh.nanoTime = 120962203520100L;
            vmh.useBytesMarshallable(true);
            wire.writeEventId(MESSAGE_HISTORY_METHOD_ID).object(SetTimeMessageHistory.class, vmh);

            assertEquals("b9 07 68 69 73 74 6f 72 79                      # history: (event)\n" +
                            "81 34 00                                        # SetTimeMessageHistory\n" +
                            "c7 73 6f 75 72 63 65 73                         # sources:\n" +
                            "82 0b 00 00 00                                  # sequence\n" +
                            "                                                # source id & index\n" +
                            "a1 01 af 02 00 00 00 00 00 00 00                # 1\n" +
                            "c7 74 69 6d 69 6e 67 73                         # timings:\n" +
                            "82 0f 00 00 00                                  # sequence\n" +
                            "                                                # timing in nanos\n" +
                            "a5 57 04                                        # 1111\n" +
                            "                                                # timing in nanos\n" +
                            "a5 ae 08                                        # 2222\n" +
                            "a7 64 0c 2c b5 03 6e 00 00 ba 80 00             # 120962203520100\n" +
                            "81 27 00 86                                     # SetTimeMessageHistory\n" +
                            "01 01 00 00 00 02 00 00 00 00 00 00 00          # sources\n" +
                            "03 57 04 00 00 00 00 00 00 ae 08 00 00 00 00 00 # timings\n" +
                            "00 64 0c 2c b5 03 6e 00 00\n",
                    bytes.toHexString(),
                    "Hex dump should include both event name and method id forms");

            // Add additional timing to the original history.
            vmh.addTiming(120962203520100L);

            VanillaMessageHistory vmh2 = new VanillaMessageHistory();
            vmh2.historyWallClock(true);

            // Deserialize the bytes back to a message history and assert its content.
            wire.read().object(vmh2, VanillaMessageHistory.class);
            vmh2.addSourceDetails(true);
            assertEquals(vmh.toString(), vmh2.toString(),
                    "Deserialised history should match original (first read)");

            VanillaMessageHistory vmh3 = new VanillaMessageHistory();
            vmh3.historyWallClock(true);

            // Deserialize the bytes again to another message history and assert its content.
            wire.read().object(vmh3, VanillaMessageHistory.class);
            vmh3.addSourceDetails(true);
            assertEquals(vmh.toString(), vmh3.toString(),
                    "Deserialised history should match original (second read)");
        }
    }

    @Test
    @DisplayName("MessageHistory should write self describing wire header")
    void testWriteHistorySelfDescribing() {
        {
            final SetTimeMessageHistory history = new SetTimeMessageHistory();
            history.useBytesMarshallable(false);
            initExampleMessageHistory(history);
            MessageHistory.set(history);

            final Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            final Wire wire = new BinaryWire(bytes);
            try (DocumentContext dc = wire.writingDocument()) {
                MessageHistory.writeHistory(dc);
            }

            assertEquals("00000000 57 00 00 00 b9 07 68 69  73 74 6f 72 79 81 4b 00 W·····hi story·K·",
                    bytes.toHexString().split("\n")[0],
                    "Self describing history should match expected header bytes");
        }
    }

    @Test
    @DisplayName("Copyable history should use self describing YAML")
    void copyableSelfDescribing() {
        assertEquals(EXPECTED_COPYABLE_YAML, doCopyableTest(false),
                "Copyable history should round-trip with self describing YAML");
    }

    @Test
    @DisplayName("Copyable history should use bytes marshalling")
    void copyableBytes() {
        assertEquals(EXPECTED_COPYABLE_YAML, doCopyableTest(true),
                "Copyable history should round-trip with bytes marshalling");
    }

    private static String doCopyableTest(boolean useBytesMarshallable) {
        SetTimeMessageHistory vmh = new SetTimeMessageHistory();
        vmh.addSource(1, 2);
        vmh.addTiming(11111111);
        vmh.addTiming(22222222);

        Wire wire = new BinaryWire();
        vmh.useBytesMarshallable(useBytesMarshallable);
        ValueOut valueOut = useBytesMarshallable
                ? wire.writeEventId(MESSAGE_HISTORY_METHOD_ID)
                : wire.writeEventName(MethodReader.HISTORY);
        valueOut.object(SetTimeMessageHistory.class, vmh);

        Wire wire2 = new YamlWire();
        wire.copyTo(wire2);

        String yaml = wire2.toString();
        verifyYamlRoundTrip(wire2);
        return yaml;
    }

    private static void verifyYamlRoundTrip(Wire wire) {
        VanillaMessageHistory vmh2 = new VanillaMessageHistory();
        vmh2.addSourceDetails(false);
        wire.read().object(vmh2, VanillaMessageHistory.class);
        assertEquals("VanillaMessageHistory { sources: [1=0x2], timings: [11111111,22222222,120962203520100], addSourceDetails=false }",
                vmh2.toString(), "Copyable history should round-trip from YAML");
    }

    @Test
    @DisplayName("MessageHistory should write bytes-marshallable header")
    void testWriteHistoryAsBytes() {
        try {
            final SetTimeMessageHistory history = new SetTimeMessageHistory();
            history.useBytesMarshallable(true);
            initExampleMessageHistory(history);
            MessageHistory.set(history);

            final Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            final Wire wire = new BinaryWire(bytes);
            try (DocumentContext dc = wire.writingDocument()) {
                MessageHistory.writeHistory(dc);
            }

            assertEquals("00000000 39 00 00 00 ba 80 00 81  33 00 86 02 01 00 00 00 9······· 3·······",
                    bytes.toHexString().split("\n")[0],
                    "Bytes-marshallable history should match expected header bytes");

            final SetTimeMessageHistory history2 = new SetTimeMessageHistory();
            initExampleMessageHistory(history2);
            MessageHistory.set(history2);

            wire.reset();
            try (DocumentContext dc = wire.writingDocument()) {
                MessageHistory.writeHistory(dc);
            }

            assertEquals("00000000 39 00 00 00 ba 80 00 81  33 00 86 02 01 00 00 00 9······· 3·······",
                    bytes.toHexString().split("\n")[0],
                    "Repeat bytes-marshallable history should match expected header bytes");

        } finally {
            MessageHistory.clear();
        }
    }

    private static void initExampleMessageHistory(VanillaMessageHistory history) {
        history.addSource(1, 0xff);
        history.addSource(2, 0xfff);
        history.addTiming(1_000_000_000_000_000_000L);
        history.addTiming(1_000_000_000_000_010_000L);
    }

    // Customized version of VanillaMessageHistory that simulates changing time.
    static class SetTimeMessageHistory extends VanillaMessageHistory {
        {
            addSourceDetails(true);
        }

        long nanoTime = 120962203520100L;

        @Override
        protected long nanoTime() {
            return nanoTime;
        }
    }
}
