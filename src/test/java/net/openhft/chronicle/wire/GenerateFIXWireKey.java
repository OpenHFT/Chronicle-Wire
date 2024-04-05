/*
 * Copyright 2016-2020 chronicle.software
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

// Class to generate FIX Wire keys from an XML file
public class GenerateFIXWireKey {

    // Main method to read and process the XML
    public static void main(String[] args) throws IOException {
        // Stream lines from the XML file
        Files.lines(Paths.get("src/test/resources/FIX42.xml"))
                // Filter lines that contain a specific field attribute
                .filter(s -> s.contains("field number='"))
                // Split each line based on the single quote character
                .map(s -> s.split("'"))
                // For each processed line, print out the key and its associated number
                .forEach(arr ->System.out.printf("\t%s(%s),%n", arr[3], arr[1]));
    }
}
