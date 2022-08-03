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
import net.openhft.chronicle.core.pool.ClassAliasPool;

public class LongConversionExampleB {
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(LongConversionExampleB.House.class);
    }
    public static class House extends SelfDescribingMarshallable{
        @LongConversion(Base64LongConverter.class)
        long owner;

        public void owner(CharSequence owner) {
            this.owner = Base64LongConverter.INSTANCE.parse(owner);
        }
    }
    public static void main(String[] args) {
        House house = new House();
        house.owner("Bill");
        System.out.println(house);
    }
}

