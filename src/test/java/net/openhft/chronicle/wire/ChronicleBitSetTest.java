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

package net.openhft.chronicle.wire;
/*
 * Copyright 2016-2021 chronicle.software
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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IOTools;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.stream.IntStream;

import static net.openhft.chronicle.core.io.Closeable.closeQuietly;
import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class ChronicleBitSetTest extends WireTestCommon {

    // Random number generator for tests
    private final Random generator = new Random();

    // The type of ChronicleBitSet to test
    private final Class clazz;

    // Collection of resources to close after tests
    private final List closeables = new ArrayList();

    // Different instances of empty ChronicleBitSets for testing
    private final ChronicleBitSet emptyBS0;
    private final ChronicleBitSet emptyBS1;
    private final ChronicleBitSet emptyBS127;
    private final ChronicleBitSet emptyBS128;

    // Capture a snapshot of all threads before test execution
    @Override
    @Before
    public void threadDump() {
        super.threadDump();
    }

    // Constructor - Initializes various configurations for the test
    public ChronicleBitSetTest(Class clazz) {
        // Ensure JVM is 64-bit for the tests
        assumeTrue(Jvm.is64bit());
        this.clazz = clazz;
        emptyBS0 = createBitSet();
        emptyBS1 = createBitSet(1);
        emptyBS127 = createBitSet(127);
        emptyBS128 = createBitSet(128);
    }

    // Helper method to assume certain conditions for the test
    private void assumeTrue(boolean bit) {
    }

    // Test data provider
    @NotNull
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        // Return test cases for different ChronicleBitSet implementations
        return Arrays.asList(new Object[][]{
                {LongArrayValueBitSet.class},
                {LongValueBitSet.class},
        });
    }

    // Cleanup resources after test
    @Override
    protected void preAfter() {
        closeQuietly(closeables);
        super.preAfter();
    }

    // Test nextSetBit() method of ChronicleBitSet
    @Test
    public void testNextSetBit0() {

        int size = 1024;
        ChronicleBitSet actual = createBitSet(size);

        ChronicleBitSet expected = createBitSet(size);
        int maxValue = Integer.MIN_VALUE;
        int minValue = Integer.MAX_VALUE;

        // Set random bits for the test
        for (int i = 0; i < 100; i++) {
            int bit = (int) (Math.random() * size);
            expected.set(bit);
            actual.set(bit);
            maxValue = Math.max(maxValue, bit);
            minValue = Math.min(minValue, bit);
        }

        int expectBit = expected.nextSetBit(0);
        int actualBit = actual.nextSetBit(0);

        // Check the correctness of the first set bit
        assertEquals(minValue, actualBit);

        // Iterate and validate each set bit
        do {
            assertEquals(expectBit, actualBit);

            expectBit = expected.nextSetBit(expectBit + 1);
            actualBit = actual.nextSetBit(actualBit + 1, maxValue);

            assertEquals(expectBit, actualBit);
        } while (expectBit != -1);
    }

    // Assert a failure with a given diagnostic message
    public void fail(String diagnostic) {
        Assert.fail(diagnostic);
    }

    // Check if the given condition is true
    public void check(boolean condition) {
        Assert.assertTrue(condition);
    }

    // Check if the given condition is true with a diagnostic message
    public void check(boolean condition, String diagnostic) {
        Assert.assertTrue(diagnostic, condition);
    }

    // Check if the ChronicleBitSet is empty
    public void checkEmpty(ChronicleBitSet s) {
        check(s.isEmpty(), "isEmpty");
        check(s.length() == 0, "length");
        check(s.cardinality() == 0, "cardinality");
        // Comparing with different empty ChronicleBitSets
        check(s.equals(emptyBS0), "equals");
        check(s.equals(emptyBS1), "equals");
        check(s.equals(emptyBS127), "equals");
        check(s.equals(emptyBS128), "equals");
        // Checking next set and clear bits
        check(s.nextSetBit(0) == -1, "nextSetBit");
        check(s.nextSetBit(127) == -1, "nextSetBit");
        check(s.nextSetBit(128) == -1, "nextSetBit");
        check(s.nextClearBit(0) == 0, "nextClearBit");
        check(s.nextClearBit(127) == 127, "nextClearBit");
        check(s.nextClearBit(128) == 128, "nextClearBit");
        // Checking string representation and get method
        check(s.toString().equals("{}"), "toString");
        check(!s.get(0), "get");
    }

    // Creates a ChronicleBitSet and sets bits based on given elements
    public ChronicleBitSet makeSet(int... elts) {
        ChronicleBitSet s = createBitSet(IntStream.of(elts).max().getAsInt() + 1L);
        for (int elt : elts)
            s.set(elt);
        return s;
    }

    // Check equality properties of two ChronicleBitSets
    public void checkEquality(ChronicleBitSet s, ChronicleBitSet t) {
        checkSanity(s, t);
        check(s.equals(t), "equals");
        check(s.toString().equals(t.toString()), "equal strings");
        check(s.length() == t.length(), "equal lengths");
        check(s.cardinality() == t.cardinality(), "equal cardinalities");
    }

    // Check the validity of ChronicleBitSet(s)
    public void checkSanity(ChronicleBitSet... sets) {
        for (ChronicleBitSet s : sets) {
            int len = s.length();
            int cardinality1 = s.cardinality();
            int cardinality2 = 0;
            // Counting set bits
            for (int i = s.nextSetBit(0); i >= 0; i = s.nextSetBit(i + 1)) {
                check(s.get(i));
                cardinality2++;
            }
            // Various validity checks
            check(s.nextSetBit(len) == -1, "last set bit");
            check(s.nextClearBit(len) == len, "last set bit");
            check(s.isEmpty() == (len == 0), "emptiness");
            check(cardinality1 == cardinality2, "cardinalities");
            check(len <= s.size(), "length <= size");
            check(len >= 0, "length >= 0");
            check(cardinality1 >= 0, "cardinality >= 0");
        }
    }

    // Performance test for the flip operation
    @Test
    @Ignore("Performance test")
    public void testFlipTime() {
        // Make a fairly random ChronicleBitSet
        ChronicleBitSet b1 = createBitSet();
        b1.set(1000);

        // Performance test for multi-word flip
        long startTime = System.currentTimeMillis();
        for (int x = 0; x < 100000; x++) {
            b1.flip(100, 900);
        }
        long endTime = System.currentTimeMillis();
        long total = endTime - startTime;
        System.out.println("Multiple word flip Time " + total);

        // Performance test for single-word flip
        startTime = System.currentTimeMillis();
        for (int x = 0; x < 100000; x++) {
            b1.flip(2, 44);
        }
        endTime = System.currentTimeMillis();
        total = endTime - startTime;
        System.out.println("Single word flip Time " + total);
    }

    @Test
    public void testNextSetBit() {
        int failCount = 0;

        // Repeat the test 100 times
        for (int i = 0; i < 100; i++) {
            int numberOfSetBits = generator.nextInt(100) + 1;
            ChronicleBitSet testSet = createBitSet(numberOfSetBits * 30);
            int[] history = new int[numberOfSetBits];

            // Set some random bits and remember them
            int nextBitToSet = 0;
            for (int x = 0; x < numberOfSetBits; x++) {
                nextBitToSet += generator.nextInt(30) + 1;
                history[x] = nextBitToSet;
                testSet.set(nextBitToSet);
            }

            // Verify their retrieval using nextSetBit()
            int historyIndex = 0;
            for (int x = testSet.nextSetBit(0); x >= 0; x = testSet.nextSetBit(x + 1)) {
                if (x != history[historyIndex++])
                    failCount++;
            }

            // Ensure the BitSet's integrity is maintained
            checkSanity(testSet);
        }

        // Check that there were no discrepancies in bit retrieval
        assertEquals(0, failCount);
    }

    @Test
    public void testNextClearBit() {
        int failCount = 0;

        // Repeat the test 1000 times
        for (int i = 0; i < 1000; i++) {
            ChronicleBitSet b = createBitSet(256);
            int[] history = new int[10];

            // Initialize the ChronicleBitSet with all set bits
            for (int x = 0; x < 256; x++)
                b.set(x);

            // Clear a sequence of random bits and remember their positions
            int nextBitToClear = 0;
            for (int x = 0; x < history.length; x++) {
                nextBitToClear += generator.nextInt(24) + 1;
                history[x] = nextBitToClear;
                b.clear(nextBitToClear);
            }

            // Verify their retrieval using nextClearBit()
            int historyIndex = 0;
            for (int x = b.nextClearBit(0); x < 256; x = b.nextClearBit(x + 1)) {
                if (x != history[historyIndex++])
                    failCount++;
            }

            // Ensure the BitSet's integrity is maintained
            checkSanity(b);
        }

        // regression test for 4350178
        ChronicleBitSet bs = createBitSet();
        // Ensure the first clear bit is at position 0
        if (bs.nextClearBit(0) != 0)
            failCount++;
        for (int i = 0; i < 64; i++) {
            bs.set(i);
            // Ensure the next clear bit is immediately after the last set bit
            if (bs.nextClearBit(0) != i + 1)
                failCount++;
        }

        // Ensure the BitSet's integrity is maintained after regression testing
        checkSanity(bs);

        // Check that there were no discrepancies in bit retrieval
        assertEquals(0, failCount);
    }

    @Test
    public void testSetGetClearFlip() {
        int failCount = 0;

        for (int i = 0; i < 100; i++) {
            ChronicleBitSet testSet = createBitSet();
            HashSet<Integer> history = new HashSet<Integer>();

            // Set a random number of bits in random places
            // up to a random maximum
            int nextBitToSet = 0;
            int numberOfSetBits = generator.nextInt(100) + 1;
            int highestPossibleSetBit = generator.nextInt(1000) + 1;
            for (int x = 0; x < numberOfSetBits; x++) {
                nextBitToSet = generator.nextInt(highestPossibleSetBit);
                history.add(nextBitToSet);
                testSet.set(nextBitToSet);
            }

            // Make sure each bit is set appropriately
            for (int x = 0; x < highestPossibleSetBit; x++) {
                if (testSet.get(x) != history.contains(x))
                    failCount++;
            }

            // Clear the bits
            Iterator<Integer> setBitIterator = history.iterator();
            while (setBitIterator.hasNext()) {
                Integer setBit = setBitIterator.next();
                testSet.clear(setBit.intValue());
            }

            // Verify they were cleared
            for (int x = 0; x < highestPossibleSetBit; x++)
                if (testSet.get(x))
                    failCount++;
            if (testSet.length() != 0)
                failCount++;

            // Set them with set(int, boolean)
            setBitIterator = history.iterator();
            while (setBitIterator.hasNext()) {
                Integer setBit = setBitIterator.next();
                testSet.set(setBit, true);
            }

            // Make sure each bit is set appropriately
            for (int x = 0; x < highestPossibleSetBit; x++) {
                if (testSet.get(x) != history.contains(x))
                    failCount++;
            }

            // Clear them with set(int, boolean)
            setBitIterator = history.iterator();
            while (setBitIterator.hasNext()) {
                Integer setBit = setBitIterator.next();
                testSet.set(setBit, false);
            }

            // Verify they were cleared
            for (int x = 0; x < highestPossibleSetBit; x++)
                if (testSet.get(x))
                    failCount++;
            if (testSet.length() != 0)
                failCount++;

            // Flip them on
            setBitIterator = history.iterator();
            while (setBitIterator.hasNext()) {
                int setBit = setBitIterator.next();
                testSet.flip(setBit);
            }

            // Verify they were flipped
            for (int x = 0; x < highestPossibleSetBit; x++) {
                if (testSet.get(x) != history.contains(x))
                    failCount++;
            }

            // Flip them off
            setBitIterator = history.iterator();
            while (setBitIterator.hasNext()) {
                int setBit = setBitIterator.next();
                testSet.flip(setBit);
            }

            // Verify they were flipped
            for (int x = 0; x < highestPossibleSetBit; x++)
                if (testSet.get(x))
                    failCount++;
            if (testSet.length() != 0)
                failCount++;

            checkSanity(testSet);
        }

        assertEquals(0, failCount);
    }

    @Test
    public void testAndNot() {
        int failCount = 0;

        for (int i = 0; i < 100; i++) {
            ChronicleBitSet b1 = createBitSet(256);
            ChronicleBitSet b2 = createBitSet(256);

            // Set some random bits in first set and remember them
            for (int x = 0; x < 32; x++)
                b1.set(generator.nextInt(256));

            // Set some random bits in second set and remember them
            for (int x = 0; x < 32; x++)
                b2.set(generator.nextInt(256));

            // andNot the sets together
            ChronicleBitSet b3 = cloneBitSet(b1);
            b3.andNot(b2);

            // Examine each bit of b3 for errors
            for (int x = 0; x < 256; x++) {
                boolean bit1 = b1.get(x);
                boolean bit2 = b2.get(x);
                boolean bit3 = b3.get(x);
                if (!(bit3 == (bit1 & (!bit2))))
                    failCount++;
            }
            checkSanity(b1, b2, b3);
        }

        assertEquals(0, failCount);
    }

    @Test
    public void testAnd() {
        int failCount = 0;

        for (int i = 0; i < 100; i++) {
            ChronicleBitSet b1 = createBitSet(256);
            if (b1 instanceof LongValueBitSet)
                assertEquals(4, b1.getWordsInUse());

            ChronicleBitSet b2 = createBitSet(256);

            // Set some random bits in first set and remember them
            for (int x = 0; x < 32; x++)
                b1.set(generator.nextInt(256));

            // Set more random bits in second set and remember them
            for (int x = 0; x < 32; x++)
                b2.set(generator.nextInt(256));

            // And the sets together
            ChronicleBitSet b3 = cloneBitSet(b1);
            b3.and(b2);

            // Examine each bit of b3 for errors
            for (int x = 0; x < 256; x++) {
                boolean bit1 = b1.get(x);
                boolean bit2 = b2.get(x);
                boolean bit3 = b3.get(x);
                if (!(bit3 == (bit1 & bit2))) {
                    System.out.println("x: " + x);
                    failCount++;
                }
            }
            checkSanity(b1, b2, b3);
        }

        assertEquals(0, failCount);
    }

    @Test
    public void testAnd2() {
        int failCount = 0;

        // Test the AND operation that clears the last word of the bitset
        ChronicleBitSet b4 = makeSet(2, 127);
        assertEquals("{2, 127}", b4.toString());
        final ChronicleBitSet b4a = makeSet(2, 64);
        assertEquals("{2, 64}", b4a.toString());
        b4.and(b4a);
        assertEquals("{2}", b4.toString());
        checkSanity(b4);
        final ChronicleBitSet bs2 = makeSet(2);
        if (!(b4.equals(bs2))) {
            failCount++;
        }

        assertEquals(0, failCount);
    }

    @Test
    public void testOr() {
        int failCount = 0;

        // Repeat the OR test 100 times
        for (int i = 0; i < 100; i++) {
            // Two bitsets of size 256
            ChronicleBitSet b1 = createBitSet(256);
            ChronicleBitSet b2 = createBitSet(256);
            int[] history = new int[64];

            // Set some random bits in first set and remember them
            int nextBitToSet = 0;
            int x = 0;
            for (; x < 32; x++) {
                nextBitToSet = generator.nextInt(256);
                history[x] = nextBitToSet;
                b1.set(nextBitToSet);
            }

            // Set more random bits in second set and remember them
            for (; x < history.length; x++) {
                nextBitToSet = generator.nextInt(256);
                history[x] = nextBitToSet;
                b2.set(nextBitToSet);
            }

            // Or the sets together
            ChronicleBitSet b3 = cloneBitSet(b1, 256);
            b3.or(b2);

            // Verify the set bits of b3 from the history
            for (int j : history) {
                if (!b3.get(j))
                    failCount++;
            }

            // Examine each bit of b3 for errors
            for (int y = 0; y < 256; y++) {
                boolean bit1 = b1.get(y);
                boolean bit2 = b2.get(y);
                boolean bit3 = b3.get(y);
                if (!(bit3 == (bit1 | bit2)))
                    failCount++;
            }
            checkSanity(b1, b2, b3);
        }

        assertEquals(0, failCount);
    }

    @Test
    public void testXor() {
        int failCount = 0;

        // Repeat the XOR test 100 times
        for (int i = 0; i < 100; i++) {
            // Two bitsets of size 256
            ChronicleBitSet b1 = createBitSet(256);
            ChronicleBitSet b2 = createBitSet(256);

            // Set some random bits in first set and remember them
            for (int x = 0; x < 32; x++)
                b1.set(generator.nextInt(256));

            // Set more random bits in second set and remember them
            for (int x = 0; x < 32; x++)
                b2.set(generator.nextInt(256));

            // Xor the sets together
            ChronicleBitSet b3 = cloneBitSet(b1);
            b3.xor(b2);

            // Examine each bit of b3 for errors
            for (int x = 0; x < 256; x++) {
                boolean bit1 = b1.get(x);
                boolean bit2 = b2.get(x);
                boolean bit3 = b3.get(x);
                if (!(bit3 == (bit1 ^ bit2)))
                    failCount++;
            }
            checkSanity(b1, b2, b3);

            // XORing b3 with itself should result in an empty set
            b3.xor(b3);
            checkEmpty(b3);
        }

        // xor that happens to clear the last word
        ChronicleBitSet b4 = makeSet(2, 64, 127);
        b4.xor(makeSet(64, 127));
        checkSanity(b4);
        if (!(b4.equals(makeSet(2))))
            failCount++;

        assertEquals(0, failCount);
    }

    @Test
    public void testEquals() {
        int failCount = 0;

        for (int i = 0; i < 100; i++) {
            // Create ChronicleBitSets of different sizes
            ChronicleBitSet b1 = createBitSet(generator.nextInt(1000) + 500);
            ChronicleBitSet b2 = createBitSet(generator.nextInt(1000) + 500);

            // Set some random bits
            int nextBitToSet = 0;
            for (int x = 0; x < 10; x++) {
                nextBitToSet += generator.nextInt(50) + 1;
                b1.set(nextBitToSet);
                b2.set(nextBitToSet);
            }

            // Verify their equality despite different storage sizes
            if (!b1.equals(b2))
                failCount++;
            checkEquality(b1, b2);
        }

        assertEquals(0, failCount);
    }

    @Test
    public void testLength() {
        int failCount = 0;

        // Test length after set
        for (int i = 0; i < 100; i++) {
            ChronicleBitSet b1 = createBitSet(256);
            int highestSetBit = 0;

            for (int x = 0; x < 100; x++) {
                int nextBitToSet = generator.nextInt(256);
                if (nextBitToSet > highestSetBit)
                    highestSetBit = nextBitToSet;
                b1.set(nextBitToSet);
                if (b1.length() != highestSetBit + 1)
                    failCount++;
            }
            checkSanity(b1);
        }

        // Test length after flip
        for (int i = 0; i < 100; i++) {
            ChronicleBitSet b1 = createBitSet(256);
            for (int x = 0; x < 100; x++) {
                // Flip a random range twice
                int rangeStart = generator.nextInt(100);
                int rangeEnd = rangeStart + generator.nextInt(100);
                b1.flip(rangeStart);
                b1.flip(rangeStart);
                if (b1.length() != 0)
                    failCount++;
                b1.flip(rangeStart, rangeEnd);
                b1.flip(rangeStart, rangeEnd);
                if (b1.length() != 0)
                    failCount++;
            }
            checkSanity(b1);
        }

        // Test length after or
        for (int i = 0; i < 100; i++) {
            ChronicleBitSet b1 = createBitSet(256);
            ChronicleBitSet b2 = createBitSet(256);
            int bit1 = generator.nextInt(100);
            int bit2 = generator.nextInt(100);
            if (bit2 >= bit1)
                bit2++;
            int highestSetBit = Math.max(bit1, bit2);
            b1.set(bit1);
            assertEquals("{" + bit1 + "}", b1.toString());
            b2.set(bit2);
            assertEquals("{" + bit2 + "}", b2.toString());
            b1.or(b2);
            final String expected = "{" + Math.min(bit1, bit2) + ", " + Math.max(bit1, bit2) + "}";
            assertEquals(expected, b1.toString());
            final int length = b1.length();
            if (length != highestSetBit + 1)
                failCount++;
            checkSanity(b1, b2);
        }

        assertEquals(0, failCount);
    }

    @Test
    public void testClear() {
        int failCount = 0;

        for (int i = 0; i < 200; i++) {
            int size = 100 + i;
            ChronicleBitSet b1 = createBitSet(size);

            // Make a fairly random ChronicleBitSet
            int numberOfSetBits = generator.nextInt(100) + 1;
            int highestPossibleSetBit = generator.nextInt(size) + 1;

            for (int x = 0; x < numberOfSetBits; x++)
                b1.set(generator.nextInt(highestPossibleSetBit));

            ChronicleBitSet b2 = cloneBitSet(b1, size);

            // Clear out a random range
            int rangeStart = generator.nextInt(100);
            int rangeEnd = rangeStart + generator.nextInt(size - rangeStart) + 1;

            // Use the clear(int, int) call on b1
            b1.clear(rangeStart, rangeEnd);

            // Use a loop on b2
            for (int x = rangeStart; x < rangeEnd; x++)
                b2.clear(x);

            // Verify their equality
            if (!b1.equals(b2)) {
                System.out.println("rangeStart = " + rangeStart);
                System.out.println("rangeEnd = " + rangeEnd);
                System.out.println("b1 = " + b1);
                System.out.println("b2 = " + b2);
                failCount++;
            }
            checkEquality(b1, b2);
        }

        assertEquals(0, failCount);
    }

    @Test
    public void testSet() {
        int failCount = 0;

        // Test set(int, int)
        for (int i = 0; i < 200; i++) {

            // Make a fairly random ChronicleBitSet
            final int size = 100 + i;
            ChronicleBitSet b1 = createBitSet(size);
            int numberOfSetBits = generator.nextInt(100) + 1;
            int possibleSetBit = generator.nextInt(size) + 1;

            for (int x = 0; x < numberOfSetBits; x++)
                b1.set(generator.nextInt(possibleSetBit));

            ChronicleBitSet b2 = cloneBitSet(b1, size);

            // Set a random range
            int rangeStart = generator.nextInt(100);
            int rangeEnd = rangeStart + generator.nextInt(size - rangeStart);

            // Use the set(int, int) call on b1
            b1.set(rangeStart, rangeEnd);

            // Use a loop on b2
            for (int x = rangeStart; x < rangeEnd; x++)
                b2.set(x);

            // Verify their equality
            if (!b1.equals(b2)) {
                System.out.println("Set 1");
                System.out.println("rangeStart = " + rangeStart);
                System.out.println("rangeEnd = " + rangeEnd);
                System.out.println("b1 = " + b1);
                System.out.println("b2 = " + b2);
                failCount++;
            }
            checkEquality(b1, b2);
        }

        // Test set(int, int, boolean)
        for (int i = 0; i < 200; i++) {

            // Make a fairly random ChronicleBitSet
            final int size = 100 + i;
            ChronicleBitSet b1 = createBitSet(size);
            int numberOfSetBits = generator.nextInt(100) + 1;
            int possibleSetBit = generator.nextInt(size) + 1;

            for (int x = 0; x < numberOfSetBits; x++)
                b1.set(generator.nextInt(possibleSetBit));

            ChronicleBitSet b2 = cloneBitSet(b1, size);
            boolean setOrClear = generator.nextBoolean();

            // Set a random range
            int rangeStart = generator.nextInt(100);
            int rangeEnd = rangeStart + generator.nextInt(size - rangeStart) + 1;

            // Use the set(int, int, boolean) call on b1
            b1.set(rangeStart, rangeEnd, setOrClear);

            // Use a loop on b2
            for (int x = rangeStart; x < rangeEnd; x++)
                b2.set(x, setOrClear);

            // Verify their equality
            if (!b1.equals(b2)) {
                System.out.println("Set 2");
                System.out.println("b1 = " + b1);
                System.out.println("b2 = " + b2);
                failCount++;
            }
            b1.set(0);
            b2.set(0);
            checkEquality(b1, b2);
        }

        assertEquals(0, failCount);
    }

    @Test
    public void testFlip() {
        int failCount = 0;

        for (int i = 0; i < 200; i++) {
            int size = 100 + i;
            ChronicleBitSet b1 = createBitSet(size);

            // Make a fairly random ChronicleBitSet
            int numberOfSetBits = generator.nextInt(100) + 1;
            int highestPossibleSetBit = generator.nextInt(size) + 1;

            for (int x = 0; x < numberOfSetBits; x++)
                b1.set(generator.nextInt(highestPossibleSetBit));

            ChronicleBitSet b2 = cloneBitSet(b1, size);

            // Flip a random range
            int rangeStart = generator.nextInt(100);
            int rangeEnd = rangeStart + generator.nextInt(size - rangeStart) + 1;

            // Use the flip(int, int) call on b1
            b1.flip(rangeStart, rangeEnd);

            // Use a loop on b2
            for (int x = rangeStart; x < rangeEnd; x++)
                b2.flip(x);

            // Verify their equality
            if (!b1.equals(b2))
                failCount++;
            checkEquality(b1, b2);
        }

        assertEquals(0, failCount);
    }

/*    @Test
    public void testGet() {
        int failCount = 0;

        for (int i = 0; i < 1000; i++) {
            ChronicleBitSet b1 = createBitSet();

            // Make a fairly random ChronicleBitSet
            int numberOfSetBits = generator.nextInt(100) + 1;
            int highestPossibleSetBit = generator.nextInt(1000) + 1;

            for (int x = 0; x < numberOfSetBits; x++)
                b1.set(generator.nextInt(highestPossibleSetBit));

            // Get a new set from a random range
            int rangeStart = generator.nextInt(100);
            int rangeEnd = rangeStart + generator.nextInt(100);

            ChronicleBitSet b2 = b1.get(rangeStart, rangeEnd);

            ChronicleBitSet b3 = createBitSet();
            for (int x = rangeStart; x < rangeEnd; x++)
                b3.set(x - rangeStart, b1.get(x));

            // Verify their equality
            if (!b2.equals(b3)) {
                System.out.println("start=" + rangeStart);
                System.out.println("end=" + rangeEnd);
                System.out.println(b1);
                System.out.println(b2);
                System.out.println(b3);
                failCount++;
            }
            checkEquality(b2, b3);
        }

        assertEquals(0, failCount);
    }*/

    @Test
    public void testIntersects() {
        int failCount = 0;

        for (int i = 0; i < 100; i++) {
            ChronicleBitSet b1 = createBitSet(256);
            ChronicleBitSet b2 = createBitSet(256);

            // Set some random bits in first set
            int nextBitToSet = 0;
            for (int x = 0; x < 30; x++) {
                nextBitToSet = generator.nextInt(256);
                b1.set(nextBitToSet);
            }

            // Set more random bits in second set
            for (int x = 0; x < 30; x++) {
                nextBitToSet = generator.nextInt(256);
                b2.set(nextBitToSet);
            }

            // Make sure they intersect
            nextBitToSet = generator.nextInt(256);
            b1.set(nextBitToSet);
            b2.set(nextBitToSet);

            if (!b1.intersects(b2))
                failCount++;

            // Remove the common set bits
            b1.andNot(b2);

            // Make sure they don't intersect
            if (b1.intersects(b2))
                failCount++;

            checkSanity(b1, b2);
        }

        assertEquals(0, failCount);
    }

    @Test
    public void testCardinality() {
        int failCount = 0;

        for (int i = 0; i < 100; i++) {
            ChronicleBitSet b1 = createBitSet(512);

            // Set a random number of increasing bits
            int nextBitToSet = 0;
            int iterations = generator.nextInt(20) + 1;
            for (int x = 0; x < iterations; x++) {
                nextBitToSet += generator.nextInt(20) + 1;
                b1.set(nextBitToSet);
            }

            if (b1.cardinality() != iterations) {
                System.out.println("Iterations is " + iterations);
                System.out.println("Cardinality is " + b1.cardinality());
                failCount++;
            }

            checkSanity(b1);
        }

        assertEquals(0, failCount);
    }

    @Test
    public void testEmpty() {
        int failCount = 0;

        // Create an empty ChronicleBitSet and ensure it's empty
        ChronicleBitSet b1 = createBitSet();
        if (!b1.isEmpty())
            failCount++;

        // Set and clear random bits multiple times and ensure the behavior of isEmpty() is correct
        int nextBitToSet = 0;
        int numberOfSetBits = generator.nextInt(100) + 1;
        int highestPossibleSetBit = generator.nextInt(1000) + 1;
        for (int x = 0; x < numberOfSetBits; x++) {
            nextBitToSet = generator.nextInt(highestPossibleSetBit);
            b1.set(nextBitToSet);
            if (b1.isEmpty())
                failCount++;
            b1.clear(nextBitToSet);
            if (!b1.isEmpty())
                failCount++;
        }

        // Ensure that no unexpected behavior was observed
        assertEquals(0, failCount);
    }

    @Test
    public void testEmpty2() {
        // Test the behavior of clear() method over a range
        {
            ChronicleBitSet t = createBitSet();
            t.set(100);
            t.clear(3, 600);
            checkEmpty(t);
        }

        // Check emptiness for multiple scenarios
        checkEmpty(emptyBS0); // Presumably a predefined empty ChronicleBitSet
        checkEmpty(createBitSet(342));
        ChronicleBitSet s = createBitSet(128);
        checkEmpty(s);
        s.clear(92);
        checkEmpty(s);
        s.clear(127, 127);
        checkEmpty(s);
        s.set(127, 127);
        checkEmpty(s);
        s.set(128, 128);
        checkEmpty(s);
        ChronicleBitSet empty = createBitSet();

        // Perform various bitwise operations with an empty set, should result in an empty set
        {
            ChronicleBitSet t = createBitSet();
            t.and(empty);
            checkEmpty(t);
        }
        {
            ChronicleBitSet t = createBitSet();
            t.or(empty);
            checkEmpty(t);
        }
        {
            ChronicleBitSet t = createBitSet();
            t.xor(empty);
            checkEmpty(t);
        }
        {
            ChronicleBitSet t = createBitSet();
            t.andNot(empty);
            checkEmpty(t);
        }
        {
            ChronicleBitSet t = createBitSet();
            t.and(t);
            checkEmpty(t);
        }
        {
            ChronicleBitSet t = createBitSet();
            t.or(t);
            checkEmpty(t);
        }
        {
            ChronicleBitSet t = createBitSet();
            t.xor(t);
            checkEmpty(t);
        }
        {
            ChronicleBitSet t = createBitSet();
            t.andNot(t);
            checkEmpty(t);
        }
        {
            ChronicleBitSet t = createBitSet();
            t.and(makeSet(1));
            checkEmpty(t);
        }
        {
            ChronicleBitSet t = createBitSet();
            t.and(makeSet(127));
            checkEmpty(t);
        }
        {
            ChronicleBitSet t = createBitSet();
            t.and(makeSet(128));
            checkEmpty(t);
        }

        // Check if flipping the same bit back and forth results in an empty set
        {
            ChronicleBitSet t = createBitSet();
            t.flip(7);
            t.flip(7);
            checkEmpty(t);
        }

        // The below tests are commented out, might be related to subsetting operations
        /*
        {
            ChronicleBitSet t = createBitSet();
            checkEmpty(t.get(200, 300));
        }
        {
            ChronicleBitSet t = makeSet(2, 5);
            check(t.get(2, 6).equals(makeSet(0, 3)), "");
        }
*/
    }

    @Test
    public void testToString() {
        // Check the string representation of an empty ChronicleBitSet
        check(createBitSet().toString().equals("{}"));

        // Check the string representation of a ChronicleBitSet with specific set bits
        check(makeSet(2, 3, 42, 43, 234).toString().equals("{2, 3, 42, 43, 234}"));

        // Check the string representation of large bit indices if enough memory is available
        // and if the instance is of LongArrayValueBitSet class
        if (Runtime.getRuntime().maxMemory() >= (512 << 20) && clazz == LongArrayValueBitSet.class) {
            // only run it if we have enough memory
            check(makeSet(Integer.MAX_VALUE - 1).toString()
                    .equals("{" + (Integer.MAX_VALUE - 1) + "}"));
            check(makeSet(Integer.MAX_VALUE).toString()
                    .equals("{" + Integer.MAX_VALUE + "}"));
            check(makeSet(0, 1, Integer.MAX_VALUE - 1, Integer.MAX_VALUE).toString()
                    .equals("{0, 1, " + (Integer.MAX_VALUE - 1) + ", " + Integer.MAX_VALUE + "}"));
        }
    }

    @Test
    public void testLogicalIdentities() {
        int failCount = 0;

        // Verify that (!b1)|(!b2) == !(b1&b2)
        for (int i = 0; i < 50; i++) {
            // Construct two fairly random ChronicleBitSets

            int numberOfSetBits = generator.nextInt(10) + 1;
            int possibleSetBit = generator.nextInt(200 - numberOfSetBits) + numberOfSetBits;

            ChronicleBitSet b1 = createBitSet(possibleSetBit);
            ChronicleBitSet b2 = createBitSet(possibleSetBit);

            for (int x = 0; x < numberOfSetBits; x++) {
                b1.set(generator.nextInt(possibleSetBit));
                b2.set(generator.nextInt(possibleSetBit));
            }

            ChronicleBitSet b3 = cloneBitSet(b1, possibleSetBit);
            ChronicleBitSet b4 = cloneBitSet(b2, possibleSetBit);

            for (int x = 0; x < possibleSetBit; x++) {
                b1.flip(x);
                b2.flip(x);
            }
            b1.or(b2);
            b3.and(b4);
            for (int x = 0; x < possibleSetBit; x++)
                b3.flip(x);
            if (!b1.equals(b3))
                failCount++;
            checkSanity(b1, b2, b3, b4);  // Presumably checks for some sanity conditions in the ChronicleBitSet instances
        }

        // Verify that (b1&(!b2)|(b2&(!b1) == b1^b2
        for (int i = 0; i < 50; i++) {
            // Construct two fairly random ChronicleBitSets

            int numberOfSetBits = generator.nextInt(10) + 1;
            int possibleSetBit = generator.nextInt(200 - numberOfSetBits) + numberOfSetBits;

            ChronicleBitSet b1 = createBitSet(possibleSetBit);
            ChronicleBitSet b2 = createBitSet(possibleSetBit);

            for (int x = 0; x < numberOfSetBits; x++) {
                b1.set(generator.nextInt(possibleSetBit));
                b2.set(generator.nextInt(possibleSetBit));
            }

            ChronicleBitSet b3 = cloneBitSet(b1, possibleSetBit);
            ChronicleBitSet b4 = cloneBitSet(b2, possibleSetBit);
            ChronicleBitSet b5 = cloneBitSet(b1, possibleSetBit);
            ChronicleBitSet b6 = cloneBitSet(b2, possibleSetBit);

            for (int x = 0; x < possibleSetBit; x++)
                b2.flip(x);
            b1.and(b2);
            for (int x = 0; x < possibleSetBit; x++)
                b3.flip(x);
            b3.and(b4);
            b1.or(b3);
            b5.xor(b6);
            if (!b1.equals(b5))
                failCount++;
            checkSanity(b1, b2, b3, b4, b5, b6);  // Checks for some sanity conditions in the ChronicleBitSet instances
        }

        // Ensure that no logical identities were violated
        assertEquals(0, failCount);
    }

    /**
     * Clone the provided ChronicleBitSet using its size.
     * @param b1 the ChronicleBitSet to clone.
     * @return the cloned ChronicleBitSet.
     */
    private ChronicleBitSet cloneBitSet(ChronicleBitSet b1) {
        return cloneBitSet(b1, b1.size());
    }

    /**
     * Clone the provided ChronicleBitSet with the given size.
     * @param b1 the ChronicleBitSet to clone.
     * @param size the size for the new cloned ChronicleBitSet.
     * @return the cloned ChronicleBitSet.
     */
    private ChronicleBitSet cloneBitSet(ChronicleBitSet b1, int size) {
        NativeBytes<Void> bytes = Bytes.allocateElasticDirect();
        IOTools.unmonitor(bytes);
        final ChronicleBitSet bitSet = createBitSet(new BinaryWire(bytes), size);
        bitSet.copyFrom(b1);
        closeables.add(bitSet);
        return bitSet;
    }

    /**
     * Create a new ChronicleBitSet with default size of 1024.
     * @return the new ChronicleBitSet.
     */
    public ChronicleBitSet createBitSet() {
        final ChronicleBitSet bitSet = createBitSet(1024);
        closeables.add(bitSet);
        return bitSet;
    }

    /**
     * Create a new ChronicleBitSet with the specified size.
     * @param bits the size for the new ChronicleBitSet.
     * @return the new ChronicleBitSet.
     */
    private ChronicleBitSet createBitSet(long bits) {
        final NativeBytes<Void> bytes = Bytes.allocateElasticDirect();
        closeables.add(bytes);
        final ChronicleBitSet bitSet = createBitSet(new BinaryWire(bytes), bits);
        closeables.add(bitSet);
        return bitSet;
    }

    /**
     * Create a ChronicleBitSet using the provided Wire and size.
     * This method uses reflection to instantiate the ChronicleBitSet.
     * @param w the Wire for the new ChronicleBitSet.
     * @param size the size for the new ChronicleBitSet.
     * @return the new ChronicleBitSet.
     */
    @NotNull
    private ChronicleBitSet createBitSet(Wire w, long size) {
        try {
            return (ChronicleBitSet) clazz.getConstructor(long.class, Wire.class).newInstance(size, w);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }
}
