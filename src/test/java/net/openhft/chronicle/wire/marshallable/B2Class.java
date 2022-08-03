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

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;

public class B2Class extends BClass {
    private static final int MASHALLABLE_VERSION = 1;

    public B2Class(int id, boolean flag, byte b, char ch, short s, int i, long l, float f, double d, String text) {
        super(id, flag, b, ch, s, i, l, f, d, text);
    }

    @Override
    public void writeMarshallable(BytesOut<?> out) {
        super.writeMarshallable(out);
        out.writeStopBit(MASHALLABLE_VERSION);
    }

    @Override
    public void readMarshallable(BytesIn<?> in) {
        super.readMarshallable(in);
        int version = (int) in.readStopBit();
        if (version == MASHALLABLE_VERSION) {
        } else {
            throw new IllegalStateException("Unknown version " + version);
        }
    }
}
