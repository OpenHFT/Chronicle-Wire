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

// Class representing a nested data structure that is self-describing.
class DMNestedClass extends SelfDescribingMarshallable {

    // String representation of the nested class.
    String str;

    // Numerical value associated with the nested class.
    int num;

    // Constructor to initialize the nested class with given values.
    public DMNestedClass(String str, int num) {
        this.str = str;
        this.num = num;
    }
}
