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

// Define the `EchoingMicroserviceMain` class which provides the entry point to initiate the echoing microservice.
public class EchoingMicroserviceMain {
    // Define the main method which serves as the entry point of the application.
    // It invokes the main method of `ChronicleServiceMain` with a predefined YAML configuration file.
    public static void main(String... args) throws IOException {
        // Call the main method of `ChronicleServiceMain` with a specific configuration file "echoing.yaml"
        // This configuration is likely defining the behavior or setup of the Chronicle service.
        ChronicleServiceMain.main("echoing.yaml");
    }
}
