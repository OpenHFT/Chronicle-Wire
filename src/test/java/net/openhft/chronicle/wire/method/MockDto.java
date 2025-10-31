/*
 * Copyright 2016-2025 chronicle.software
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
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.wire.Comment;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;

class MockDto extends SelfDescribingMarshallable {
    @Comment("field1 comment")
    String field1;
    @Comment("field2 comment")
    double field2;
}
