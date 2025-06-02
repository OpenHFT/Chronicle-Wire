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

// Class WMTwoFields, a data structure that extends SelfDescribingMarshallable
// to utilize its serialization/deserialization and other common functionalities.
class WMTwoFields extends SelfDescribingMarshallable {

    // 'id' field with a custom long conversion using WordsLongConverter,
    // facilitating specific serialization/deserialization of the integer as a long.
    @LongConversion(WordsLongConverter.class)
    int id;

    // 'ts' field (presumably for timestamp) with a custom long conversion using
    // MicroTimestampLongConverter, facilitating specific serialization/deserialization
    // of the long value, typically to/from a microsecond-precision timestamp.
    @LongConversion(MicroTimestampLongConverter.class)
    long ts;
}
