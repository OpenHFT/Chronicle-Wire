/*
 * Copyright 2016-2020 chronicle.software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

// This class provides tests for checking the default zero licence.
public class DefaultZeroLicenceTest extends WireTestCommon {

    // Test the default zero licence check to ensure correct behaviour.
    @Test
    public void testLicenceCheck() {
        // Expect an exception with a specific message pointing to a license issue
        expectException(
            e -> e.throwable != null && e.throwable.getMessage().contains("Please contact sales@chronicle.software"),
            "license check"
        );

        // Allocating elastic byte buffer
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        try {
            // Applying the DEFAULT_ZERO_BINARY wire type to bytes, expecting it to fail due to license restrictions
            WireType.DEFAULT_ZERO_BINARY.apply(bytes);
            fail();
        } catch (IllegalStateException e) {
            // Checking the exception message for the expected licensing message
            assertTrue(e.getMessage().contains(
                    "A Chronicle Wire Enterprise licence is required to run this code"));
        } finally {
            // Releasing the byte buffer to free up resources
            bytes.releaseLast();
        }
    }
}
