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

package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.wire.IMid;

import java.util.List;

/**
 * Interface defining a set of mock methods for testing purposes.
 * It includes methods with various argument types and one that returns another interface.
 */
interface MockMethods {
    // Method with a single MockDto argument
    void method1(MockDto dto);

    // Another method with a single MockDto argument
    void method2(MockDto dto);

    // Method with a list of MockDto objects as an argument
    void method3(List<MockDto> dtos);

    // Method with a list of strings as an argument
    void list(List<String> strings);

    // Method designed to throw an exception
    void throwException(String s);

    // Method that returns an instance of the IMid interface
    IMid mid(String text);
}
