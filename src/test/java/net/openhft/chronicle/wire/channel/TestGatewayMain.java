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

package net.openhft.chronicle.wire.channel;

import java.io.IOException;

/**
 * TestGatewayMain serves as a testing entry point to the application,
 * delegating its start through ChronicleGatewayMain's main method.
 * This class is intended to be used within a test classpath scope.
 */
public class TestGatewayMain {

    /**
     * Main method to launch the test gateway.
     *
     * @param args Command-line arguments to be passed to ChronicleGatewayMain.
     * @throws IOException if an I/O error occurs in ChronicleGatewayMain.
     */
    public static void main(String[] args) throws IOException {
        // Delegates the call to ChronicleGatewayMain's main method,
        // effectively starting the application with the provided arguments.
        ChronicleGatewayMain.main(args);
    }
}
