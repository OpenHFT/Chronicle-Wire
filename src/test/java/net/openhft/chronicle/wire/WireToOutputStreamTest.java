/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"deprecation", "removal"})
@SuppressFBWarnings(
        value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
        justification = "Fields are populated via Wire marshalling in tests.")
public class WireToOutputStreamTest extends WireTestCommon {

    private WireType currentWireType;

    // Constructor to initialize the parameter
    public void initWireToOutputStreamTest(WireType currentWireType) {
        this.currentWireType = currentWireType;
    }

    // Parameters for the test
    public static Collection<WireType> data() {
        List<WireType> wireTypes = new ArrayList<>();
        // populate wireTypes based on availability and certain conditions
        for (WireType wireType : WireType.values()) {
            if (wireType.isAvailable()
                    && wireType != WireType.RAW // Serializable objects are not support for RAW binary
                    && wireType != WireType.CSV // type literals not supported in CSV files
                    && wireType != WireType.READ_ANY // cannot write to a READ_ANY until it knows what type to use.
            ) {
                wireTypes.add(wireType);
            }
        }

        return wireTypes;
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{index}: {0}")
    @DisplayName("Wire output stream round trips timestamps")
    // Test to ensure the Timestamp object can be serialized and deserialized correctly
    public void testTimestamp(WireType currentWireType) {
        initWireToOutputStreamTest(currentWireType);
        final Wire wire = currentWireType.apply(Bytes.allocateElasticOnHeap(128));
        final Timestamp ts = new Timestamp(1234567890000L);
        wire.write().object(ts);

        Timestamp ts2 = wire.read()
                .object(Timestamp.class);
        assertEquals(ts.toString(),
                ts2.toString(),
                "Timestamp should serialise and deserialise via wire type " + currentWireType);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{index}: {0}")
    @DisplayName("Wire output stream round trips without sockets")
    // Test serialization and deserialization without a socket
    public void testNoSocket(WireType currentWireType) {
        initWireToOutputStreamTest(currentWireType);
        final Wire wire = currentWireType.apply(Bytes.allocateElasticOnHeap(128));
        final AnObject ao = writeAnObject(wire);

        Object ao2 = readAnObject(wire);
        assertEquals(ao.toString(),
                ao2.toString(),
                "Object should serialise and deserialise without sockets for wire type " + currentWireType);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{index}: {0}")
    @DisplayName("Wire output stream round trips with sockets")
    // Test serialization and deserialization using a socket
    public void testVisSocket(WireType currentWireType) throws IOException {
        initWireToOutputStreamTest(currentWireType);
        try (ServerSocket ss = new ServerSocket(0);
             Socket s = new Socket("localhost", ss.getLocalPort());
             Socket s2 = ss.accept()) {
            final WireToOutputStream wtos = new WireToOutputStream(currentWireType, s.getOutputStream());

            final Wire wire = wtos.getWire();
            final AnObject ao = writeAnObject(wire);
            wtos.flush();

            final InputStreamToWire istw = new InputStreamToWire(currentWireType, s2.getInputStream());
            final Wire wire2 = istw.readOne();
            final Object ao2 = readAnObject(wire2);
            assertEquals(ao.toString(),
                    ao2.toString(),
                    "Object should serialise and deserialise via socket stream for wire type " + currentWireType);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @NotNull
    // Function to read an object from the wire
    private Object readAnObject(Wire wire2) {
        Class<?> type = wire2.getValueIn().typeLiteral();
        Object ao2 = ObjectUtils.newInstance(type);
        Wires.readMarshallable(ao2, wire2, true);
        return ao2;
    }

    @NotNull
    // Function to write an object to the wire
    private AnObject writeAnObject(Wire wire) {
        AnObject ao = new AnObject();
        ao.value = 12345;
        ao.text = "Hello";
        //ao.timestamp1 = new Timestamp(1234567890);
        // write the type is needed.
        wire.getValueOut().typeLiteral(AnObject.class);
        Wires.writeMarshallable(ao, wire);
        return ao;
    }

    // Serializable class for testing
    public static class AnObject implements Serializable {
        private static final long serialVersionUID = 0L;
        long value;
        String text;

        final Timestamp timestamp = new Timestamp(1234567890000L);

        @Override
        public String toString() {
            // Override toString for easier debugging
            return "AnObject{" +
                    "value=" + value +
                    ", text='" + text + '\'' +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
}
