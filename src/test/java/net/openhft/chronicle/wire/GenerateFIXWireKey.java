//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
