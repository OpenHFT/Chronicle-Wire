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
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.FieldGroup;
import java.nio.ByteBuffer;
import static net.openhft.chronicle.bytes.Bytes.*;
import static net.openhft.chronicle.wire.WireType.*;
import net.openhft.chronicle.core.pool.ClassAliasPool;

public class LongConversionExampleC {
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(House.class);
    }
    public static class House extends SelfDescribingMarshallable {
        @FieldGroup("address")
        // 5 longs, each at 8 bytes = 40 bytes, so we can store a String with up to 40 ISO-8859 characters
        private long text4a, text4b, text4c, text4d, text4e;
        private transient Bytes address = Bytes.forFieldGroup(this, "address");

        public void address(CharSequence owner) {
            address.append(owner);
        }
    }
    public static void main(String[] args) {
        House house = new House();
        house.address("82 St John Street, Clerkenwell, London");

        // creates a buffer to store bytes
        final Bytes<ByteBuffer> t = elasticHeapByteBuffer();

        // the encoding format
        final Wire wire = BINARY.apply(t);

        // writes the house object to the bytes
        wire.getValueOut().object(house);

        // dumps out the contents of the bytes
        System.out.println(t.toHexString());
        System.out.println(t);

        // reads the house object from the bytes
        final House object = wire.getValueIn().object(House.class);

        // prints the value of text4
        System.out.println(object.address);
    }
}
