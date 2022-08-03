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

package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Assert;
import org.junit.Test;

public class JavaSourceCodeFormatterTest extends WireTestCommon {

    @Test
    public void testAppend() {
        Assert.assertEquals("" +
                        "public Appendable append(final CharSequence csq) {\n" +
                        "    return sb.append(replaceNewLine(csq, 0, csq.length() - 1));\n" +
                        "}\n",
                new JavaSourceCodeFormatter()
                        .append("public Appendable append(final CharSequence csq) {\n")
                        .append("return sb.append(replaceNewLine(csq, 0, csq.length() - 1));\n")
                        .append('}').append('\n')
                        .toString());
    }
}