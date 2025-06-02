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

/**
 * This class acts as a nested data structure
 * which is self-describing, enabling simplified marshalling and unmarshalling
 * while interacting with byte streams or while serialization.
 */
class DMNestedClass extends SelfDescribingMarshallable {

    // A string attribute to hold textual data within the instance of DMNestedClass.
    String str;

    // An integer attribute to hold numerical data within the instance of DMNestedClass.
    int num;

    /**
     * Parameterized constructor to initialize an instance of DMNestedClass
     * with specified values.
     *
     * @param str a String, representing the textual data to be held by the instance.
     * @param num an int, representing the numerical data to be held by the instance.
     */
    public DMNestedClass(String str, int num) {
        this.str = str;  // Assign the provided string to the instance variable 'str'.
        this.num = num;  // Assign the provided integer to the instance variable 'num'.
    }
}
