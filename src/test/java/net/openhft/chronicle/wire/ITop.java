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

// An interface that defines various methods with different parameter requirements.
interface ITop {
    // Define a method that takes a string and returns an IMid object.
    IMid mid(String name);

    // Define a method that takes a string and returns an IMid2 object.
    IMid2 mid2(String name);

    // Define a method with no arguments that returns an IMid object.
    IMid midNoArg();

    // Define a method that takes an int and a long and returns an IMid object.
    IMid midTwoArgs(int i, long l);
}
