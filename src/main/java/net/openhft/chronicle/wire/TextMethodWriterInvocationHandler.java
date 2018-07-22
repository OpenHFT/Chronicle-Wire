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
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/*
 * Created by Peter Lawrey on 25/03/16.
 */
public class TextMethodWriterInvocationHandler extends AbstractMethodWriterInvocationHandler {
    // TODO remove this hack for TextMethodTester
    static boolean ENABLE_EOD = true;
    @NotNull
    private final TextWire wire;
    private final Map<Method, Consumer<Object[]>> visitorConverter = new LinkedHashMap<>();

    TextMethodWriterInvocationHandler(@NotNull TextWire wire) {
        this.wire = wire;
        recordHistory = wire.recordHistory();
    }

    @Override
    protected void handleInvoke(Method method, Object[] args) {
        visitorConverter.computeIfAbsent(method, this::buildConverter)
                .accept(args);
        handleInvoke(method, args, wire);
        wire.getValueOut().resetBetweenDocuments();
        Bytes<?> bytes = wire.bytes();
        if (bytes.peekUnsignedByte(bytes.writePosition() - 1) >= ' ')
            bytes.append('\n');
        if (ENABLE_EOD) {
            bytes.append("---\n");
        }
    }

    private Consumer<Object[]> buildConverter(Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        if (parameterAnnotations.length <= 0)
            return NoOp.INSTANCE;
        for (Annotation anno : parameterAnnotations[0]) {
            if (anno instanceof LongConversion) {
                LongConversion longConversion = (LongConversion) anno;
                LongConverter ic = ObjectUtils.newInstance(longConversion.value());
                return a -> {
                    if (a[0] instanceof Number) {
                        StringBuilder sb = Wires.acquireStringBuilder();
                        ic.append(sb, ((Number) a[0]).longValue());
                        a[0] = sb.toString();
                    }
                };
            }
            if (anno instanceof IntConversion) {
                IntConversion intConversion = (IntConversion) anno;
                IntConverter ic = ObjectUtils.newInstance(intConversion.value());
                return a -> {
                    if (a[0] instanceof Number) {
                        StringBuilder sb = Wires.acquireStringBuilder();
                        ic.append(sb, ((Number) a[0]).intValue());
                        a[0] = sb.toString();
                    }
                };
            }
        }
        return NoOp.INSTANCE;
    }

    enum NoOp implements Consumer<Object[]> {
        INSTANCE;

        @Override
        public void accept(Object[] objects) {
        }
    }
}
