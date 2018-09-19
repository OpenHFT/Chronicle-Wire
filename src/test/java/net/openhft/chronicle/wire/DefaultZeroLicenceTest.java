/*
 * Copyright 2016 higherfrequencytrading.com
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
import net.openhft.chronicle.bytes.BytesUtil;
import org.junit.After;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Rob Austin.
 */
public class DefaultZeroLicenceTest {

    @Test
    public void testLicenceCheck() {
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        try {
            WireType.DEFAULT_ZERO_BINARY.apply(bytes);
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains(
                    "A Chronicle Wire Enterprise licence is required to run this code"));
        } finally {
            bytes.release();
        }
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }
}
