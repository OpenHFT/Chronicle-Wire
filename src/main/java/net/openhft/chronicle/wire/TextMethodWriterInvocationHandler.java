/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

/**
 * Created by peter on 25/03/16.
 */
public class TextMethodWriterInvocationHandler extends AbstractMethodWriterInvocationHandler {
    // TODO remove this hack for TextMethodTester
    static boolean ENABLE_EOD = true;
    @NotNull
    private final TextWire wire;

    TextMethodWriterInvocationHandler(@NotNull TextWire wire) {
        this.wire = wire;
        recordHistory = wire.recordHistory();
    }

    @Override
    protected void handleInvoke(Method method, Object[] args) {
        handleInvoke(method, args, wire);
        wire.getValueOut().resetBetweenDocuments();
        Bytes<?> bytes = wire.bytes();
        if (bytes.peekUnsignedByte(bytes.writePosition() - 1) >= ' ')
            bytes.append('\n');
        if (ENABLE_EOD) {
            bytes.append("---\n");
        }
    }
}
