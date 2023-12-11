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

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

interface Flow1 {
    Flow2 first(String args);
}

interface Flow2 {
    Flow3 second(long num);
}

interface Flow3 {
    void third(List<String> list);
}

@Ignore
public class FlowMethodTest extends WireTestCommon {
    @SuppressWarnings("rawtypes")
    @Test
    public void runYaml() throws IOException {
        TextMethodTester test = new YamlMethodTester<>(
                "flow-in.yaml",
                out -> out,
                Flow1.class,
                "flow-in.yaml")
                .setup("flow-in.yaml") // calls made here are not validated in the output.
                .run();
        assertEquals(test.expected(), test.actual());
    }
}
