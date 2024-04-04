package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.wire.Comment;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;

class MockDto extends SelfDescribingMarshallable {
    @Comment("field1 comment")
    String field1;
    @Comment("field2 comment")
    double field2;
}
