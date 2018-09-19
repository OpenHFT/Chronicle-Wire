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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/*
 * Created by Peter Lawrey on 06/10/15.
 */
public class GenerateFIXWireKey {
    public static void main(String[] args) throws IOException {
        Files.lines(Paths.get("src/test/resources/FIX42.xml"))
                .filter(s -> s.contains("field number='"))
                .map(s -> s.split("'"))
                .forEach(arr -> System.out.printf("\t%s(%s),%n", arr[3], arr[1]));
    }
}
