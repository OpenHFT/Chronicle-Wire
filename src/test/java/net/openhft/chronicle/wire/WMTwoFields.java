/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
