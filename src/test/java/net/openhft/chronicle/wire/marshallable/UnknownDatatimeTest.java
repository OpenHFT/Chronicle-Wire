/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class UnknownDatatimeTest extends WireTestCommon {
    @Test
    public void ignoreAnUnknownDateTime() throws IOException {
        AClass aClass = Marshallable.fromString("!" + AClass.class.getName() + " { eventTime: 2019-04-02T11:20:41.616653, id: 123456 }");
        assertEquals(123456, aClass.id);
    }
}
