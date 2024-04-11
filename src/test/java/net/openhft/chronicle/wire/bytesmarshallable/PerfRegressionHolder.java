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

package net.openhft.chronicle.wire.bytesmarshallable;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.wire.BytesInBinaryMarshallable;

import java.io.File;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.Arrays;
import java.util.stream.Stream;

import static net.openhft.chronicle.core.UnsafeMemory.MEMORY;

public class PerfRegressionHolder {
    // A set of test strings split into an array
    String[] s = "1,12,12345,123456789,123456789012,12345678901234567890123".split(",");
    // Various field initializations using different constructors or default constructors
    // for different types of BytesFields and StringFields
    BytesFields bf1 = new BytesFields(s);
    BytesFields bf2 = new BytesFields();

    DefaultBytesFields df1 = new DefaultBytesFields(s);
    DefaultBytesFields df2 = new DefaultBytesFields();

    ReferenceBytesFields rf1 = new ReferenceBytesFields(s);
    ReferenceBytesFields rf2 = new ReferenceBytesFields();

    StringFields sf1 = new StringFields(s);
    StringFields sf2 = new StringFields();

    DefaultStringFields dsf1 = new DefaultStringFields(s);
    DefaultStringFields dsf2 = new DefaultStringFields();

    ArrayStringFields asf1 = new ArrayStringFields(s);
    ArrayStringFields asf2 = new ArrayStringFields();

    DefaultUtf8StringFields dusf1 = new DefaultUtf8StringFields(s);
    DefaultUtf8StringFields dusf2 = new DefaultUtf8StringFields();

    DefaultStringFields dsf0 = new DefaultStringFields(new String[6]);

    // Initializing byte buffers: one direct (off-heap) and one on heap
    final Bytes<?> direct = Bytes.allocateElasticDirect();
    final Bytes<?> onHeap = Bytes.allocateElasticOnHeap();

    // MappedBytes will be used to map files into memory for testing
    MappedBytes mapped;

    // A volatile integer barrier utilized possibly for ensuring visibility between threads
    static volatile int barrier;

    // Method to perform a performance test using a provided Runnable
    public void doTest(Runnable runnable) {
        // Creating a temporary file and ensuring it will be deleted upon JVM exit
        File tmpFile = IOTools.createTempFile("regressionTest");
        tmpFile.deleteOnExit();
        try {
            // Attempting to create a memory-mapped byte buffer from the temporary file
            try (MappedBytes mapped = MappedBytes.mappedBytes(tmpFile, 64 << 10)) {
                this.mapped = mapped;
                // Variables defining the number of test runs and outlier limit
                int runs = 20_000;
                int outlier = Jvm.isArm() ? 500_000 : 100_000;

                long[] times = new long[4];
                // Running the test several times, recording execution times
                // (Note: We'd need to see the implementation of Jvm and IOTools for complete clarity)
                for (int i = 0; i < times.length; i++) {
                    long time = 0;
                    for (int r = 0; r < runs; r++) {
                        long start = System.nanoTime();
                        runnable.run();
                        barrier++;
                        long end = System.nanoTime();
                        time += Math.min(outlier, end - start);
                    }
                    // Display the measured times and pause execution for 100ms between runs
                    System.out.println("times " + i + ": " + time / runs);
                    runs = 100_000;
                    times[i] = time / runs;
                    Jvm.pause(100);
                }
                // Sorting times and displaying the median result
                Arrays.sort(times);
                System.out.println("result: " + times[(times.length - 1) / 2] + " us");
            }
        } catch (IOException ioe) {
            throw new AssertionError(ioe);
        }
    }

    // Inner class representing a type of marshallable byte fields for testing
    static class BytesFields extends BytesInBinaryMarshallable {
        // Several Bytes objects used for testing purposes
        Bytes<?> a = Bytes.allocateElasticOnHeap();
        Bytes<?> b = Bytes.allocateElasticOnHeap();
        Bytes<?> c = Bytes.allocateElasticOnHeap();
        Bytes<?> d = Bytes.allocateElasticOnHeap();
        Bytes<?> e = Bytes.allocateElasticOnHeap();
        Bytes<?> f = Bytes.allocateElasticOnHeap();

        // Default constructor
        public BytesFields() {
        }

        // Constructor to initialize Bytes objects with provided strings
        public BytesFields(String... s) {
            this();
            // Assigning provided strings to byte fields
            this.a.append(s[0]);
            this.b.append(s[1]);
            this.c.append(s[2]);
            this.d.append(s[3]);
            this.e.append(s[4]);
            this.f.append(s[5]);
        }
    }

    // A static inner class that extends BytesFields and presumably defaults in its behavior
    // in reading and writing marshallable bytes fields from and to byte sequences.
    static class DefaultBytesFields extends BytesFields {
        // Default constructor
        public DefaultBytesFields() {
        }

        // Constructor that takes variable string arguments and forwards them to the super constructor
        public DefaultBytesFields(String... s) {
            super(s);
        }

        // Read marshallable data from a byte sequence, with specific bytes fields
        @Override
        public void readMarshallable(BytesIn<?> bytes) throws IORuntimeException, BufferUnderflowException, IllegalStateException {
            // Reading 8-bit sequences and appending to each of the Bytes fields
            read8Bit(bytes, a);
            read8Bit(bytes, b);
            read8Bit(bytes, c);
            read8Bit(bytes, d);
            read8Bit(bytes, e);
            read8Bit(bytes, f);
        }

        // Reads an 8-bit byte sequence from the provided byte sequence and appends it to the provided Bytes field
        protected void read8Bit(BytesIn<?> bytes, Bytes<?> a) {
            bytes.read8bit(a);
        }

        // Write marshallable data to a byte sequence, with specific bytes fields
        @Override
        public void writeMarshallable(BytesOut<?> bytes) throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException {
            // Writing each of the Bytes fields as 8-bit sequences to the provided byte sequence
            write8Bit(bytes, a);
            write8Bit(bytes, b);
            write8Bit(bytes, c);
            write8Bit(bytes, d);
            write8Bit(bytes, e);
            write8Bit(bytes, f);
        }

        // Writes the provided BytesStore to the provided byte sequence as an 8-bit byte sequence
        @SuppressWarnings("rawtypes")
        protected void write8Bit(BytesOut<?> bytes, BytesStore<?, ?> a) {
            if (a == null) {
                bytes.writeStopBit(-1);
            } else {
                long offset = a.readPosition();
                long readRemaining = Math.min(bytes.writeRemaining(), a.readLimit() - offset);
                bytes.writeStopBit(readRemaining);
                try {
                    bytes.write(a, offset, readRemaining);
                } catch (BufferUnderflowException | IllegalArgumentException e1) {
                    throw new AssertionError(e1);
                }
            }
        }
    }

    // A static inner class extending DefaultBytesFields, potentially with a focus on
    // referencing behaviors during reading and writing of byte sequences.
    static class ReferenceBytesFields extends DefaultBytesFields {
        // Default constructor
        public ReferenceBytesFields() {
        }

        // Constructor that takes variable string arguments and forwards them to the super constructor
        public ReferenceBytesFields(String... s) {
            super(s);
        }

        // ORead marshallable data from a byte sequence, with specific bytes fields,
        // with a focus on individual byte reading to support reference-like behavior.
        @Override
        public void readMarshallable(BytesIn<?> bytes) throws IORuntimeException, BufferUnderflowException, IllegalStateException {
            // Reading each byte individually and appending it to each of the Bytes fields
            read8Bit(bytes, a);
            read8Bit(bytes, b);
            read8Bit(bytes, c);
            read8Bit(bytes, d);
            read8Bit(bytes, e);
            read8Bit(bytes, f);
        }

        // Write marshallable data to a byte sequence, with specific bytes fields,
        // with a focus on individual byte writing to support reference-like behavior.
        @Override
        public void writeMarshallable(BytesOut<?> bytes) throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException {
            // Writing each byte individually from each of the Bytes fields to the provided byte sequence
            write8Bit(bytes, a);
            write8Bit(bytes, b);
            write8Bit(bytes, c);
            write8Bit(bytes, d);
            write8Bit(bytes, e);
            write8Bit(bytes, f);
        }

        // Read an 8-bit byte sequence from the provided byte sequence,
        // specifically reading the length first (as stop-bit encoded), and then reading
        // each subsequent byte individually and appending them to the provided Bytes field.
        @Override
        protected void read8Bit(BytesIn<?> bytes, Bytes<?> a) {
            int length = (int) bytes.readStopBit();
            a.clear();
            for (int i = 0; i < length; i++)
                a.writeByte(bytes.readByte());
        }

        // Writes the provided BytesStore to the provided byte sequence
        // by first writing the length of the bytes to be written (as stop-bit encoded),
        // and then writing each byte individually.
        @SuppressWarnings("rawtypes")
        @Override
        protected void write8Bit(BytesOut<?> bytes, BytesStore<?, ?> a) {
            final int length = a.length();
            bytes.writeStopBit(length);
            for (int i = 0; i < length; i++)
                bytes.writeByte(a.readByte(i));
        }
    }

    // A static class extending BytesInBinaryMarshallable designed to manage multiple String fields,
    // while also providing functionality to read and write these fields to and from byte sequences.
    static class StringFields extends BytesInBinaryMarshallable {
        // String fields to be managed by this class
        String a = "";
        String b = "";
        String c = "";
        String d = "";
        String e = "";
        String f = "";

        public StringFields() {

        }

        // Constructor taking a variable number of String arguments and initializing respective fields.
        public StringFields(String... s) {
            this();  // Calling the default constructor
            // Initializing each field with corresponding input
            this.a = s[0];
            this.b = s[1];
            this.c = s[2];
            this.d = s[3];
            this.e = s[4];
            this.f = s[5];
        }
    }

    // A static class extending StringFields that leverages the Unsafe API to directly
    // manipulate memory offsets, aiming to enhance the performance in the reading/writing operations.
    static class ArrayStringFields extends StringFields {
        // Static array holding memory offsets for each String field in StringFields class
        static final long[] offsets = Stream.of(StringFields.class.getDeclaredFields())
                .filter(f -> f.getType() == String.class)  // Filtering only String fields
                .mapToLong(UnsafeMemory::unsafeObjectFieldOffset)  // Getting memory offsets
                .toArray();  // Collecting to array

        // Default constructor
        public ArrayStringFields() {
            super();
        }

        // Constructor accepting variable String arguments and forwarding them to the superclass
        public ArrayStringFields(String... s) {
            super(s);
        }
 // UU 2340
 // 8U 3110
 // U8 3060
 // 88 3510

        // Read marshallable data from a byte sequence into String fields
        // using memory offsets for potentially enhanced performance.
        @Override
        public void readMarshallable(BytesIn<?> bytes) throws IORuntimeException, BufferUnderflowException, IllegalStateException {
            for (long offset : offsets)
                MEMORY.putObject(this, offset, bytes.read8bit());
        }

        // Write String fields to a byte sequence using memory offsets
        // and writing each string twice in different formats (8bit and UTF8) consecutively.
        @Override
        public void writeMarshallable(BytesOut<?> bytes) throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException {
            for (long offset : offsets) {
                final String s = MEMORY.getObject(this, offset);
                bytes.write8bit(s);  // Writing string in 8-bit format
                bytes.writeUtf8(s);  // Writing string in UTF-8 format
            }
        }
    }

    // A static class extending StringFields to manage String fields with a default read and write implementation,
    // avoiding the direct memory manipulation used in ArrayStringFields.
    static class DefaultStringFields extends StringFields {
        // Default constructor
        public DefaultStringFields() {
        }

        // Constructor accepting variable String arguments and forwarding them to the superclass
        public DefaultStringFields(String... s) {
            super(s);
        }

        // Read marshallable data from a byte sequence into String fields
        // using direct field assignment, presumably for standard use cases where direct memory
        // manipulation is unnecessary or unsafe.
        @Override
        public void readMarshallable(BytesIn<?> bytes) throws IORuntimeException, BufferUnderflowException, IllegalStateException {
            a = bytes.read8bit();
            b = bytes.read8bit();
            c = bytes.read8bit();
            d = bytes.read8bit();
            e = bytes.read8bit();
            f = bytes.read8bit();
        }

        // Write String fields to a byte sequence using direct field access
        // and writing each string once in an 8bit format.
        @Override
        public void writeMarshallable(BytesOut<?> bytes) throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException {
            bytes.write8bit(a);
            bytes.write8bit(b);
            bytes.write8bit(c);
            bytes.write8bit(d);
            bytes.write8bit(e);
            bytes.write8bit(f);
        }
    }

    // A static class extending StringFields to manage String fields and
    // providing default read and write implementations for UTF-8 encoded strings.
    static class DefaultUtf8StringFields extends StringFields {
        // Default constructor
        public DefaultUtf8StringFields() {
        }

        // Constructor accepting variable String arguments and forwarding them to the superclass
        public DefaultUtf8StringFields(String... s) {
            super(s);
        }

        // Overridden method to read marshallable data from a byte sequence into String fields
        // using UTF-8 encoding for string deserialization.
        @Override
        public void readMarshallable(BytesIn<?> bytes) throws IORuntimeException, BufferUnderflowException, IllegalStateException {
            a = bytes.readUtf8();
            b = bytes.readUtf8();
            c = bytes.readUtf8();
            d = bytes.readUtf8();
            e = bytes.readUtf8();
            f = bytes.readUtf8();
        }

        // Overridden method to write String fields to a byte sequence using
        // UTF-8 encoding for string serialization.
        @Override
        public void writeMarshallable(BytesOut<?> bytes) throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException {
            bytes.writeUtf8(a);
            bytes.writeUtf8(b);
            bytes.writeUtf8(c);
            bytes.writeUtf8(d);
            bytes.writeUtf8(e);
            bytes.writeUtf8(f);
        }

    }

    // Example test method for benchmarking a specific type of object
    public void benchNull() {
        final DefaultStringFields from = this.dsf0;
        final DefaultStringFields to = this.dsf2;
        testAll2(from, to);  // Utilizing a variant of the testing method
    }

    public void benchBytes() {
        testAll(this.df1, this.df2);
    }

    public void benchFields() {
        testAll(this.bf1, this.bf2);
    }

    public void benchRefBytes() {
        testAll(this.rf1, this.rf2);
    }

    public void benchString() {
        testAll(this.dsf1, this.dsf2);
    }

    public void benchArrayString() {
        testAll(this.asf1, this.asf2);
    }

    public void benchUtf8String() {
        testAll(this.dusf1, this.dusf2);
    }

    public void benchRefString() {
        testAll(this.sf1, this.sf2);
    }

    // Method to perform a basic test, writing and then reading data for various buffer types
    private void testAll(BytesMarshallable from, BytesMarshallable to) {
        onHeap.clear();
        from.writeMarshallable(onHeap);
        to.readMarshallable(onHeap);

        direct.clear();
        from.writeMarshallable(direct);
        to.readMarshallable(direct);

        mapped.clear();
        from.writeMarshallable(mapped);
        to.readMarshallable(mapped);
    }

    // Method to perform a variation of the basic test, performing multiple read operations
    // after a single write for each buffer type
    private void testAll2(BytesMarshallable from, BytesMarshallable to) {
        onHeap.clear();
        from.writeMarshallable(onHeap);
        for (int j = 0; j < 4; j++) {
            onHeap.readPosition(0);
            to.readMarshallable(onHeap);
        }

        direct.clear();
        from.writeMarshallable(direct);
        for (int j = 0; j < 4; j++) {
            direct.readPosition(0);
            to.readMarshallable(direct);
        }

        mapped.clear();
        from.writeMarshallable(mapped);
        for (int j = 0; j < 4; j++) {
            mapped.readPosition(0);
            to.readMarshallable(mapped);
        }
    }
}
