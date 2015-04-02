/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

@FunctionalInterface
public interface ReadMarshallable {
    /**
     * Straight line ordered decoding.
     *
     * @param wire to read from in an ordered manner.
     * @throws IllegalStateException the stream wasn't ordered or formatted as expected.
     */
    void readMarshallable(WireIn wire) throws IllegalStateException;
}
