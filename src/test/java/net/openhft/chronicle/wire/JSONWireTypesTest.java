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
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static net.openhft.chronicle.wire.WireType.JSON_ONLY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class JSONWireTypesTest extends WireTestCommon {
    @SuppressWarnings("unchecked")
    @Test
    public void nestedSets() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        DtoWithNestedSets dto = new DtoWithNestedSets()
            .setOfSets(
                of(of(new Dto().field("123")), of(new Dto().field("234")))
            );

        String dtoAsJson = JSON_ONLY.asString(dto);
        assertEquals(
                "{\"@net.openhft.chronicle.wire.JSONWireTypesTest$DtoWithNestedSets\":{" +
                        "\"setOfSets\":[ " +
                        "{\"@!set\":[ {\"@net.openhft.chronicle.wire.JSONWireTypesTest$Dto\":{\"field\":\"234\"}} ]}," +
                        "{\"@!set\":[ {\"@net.openhft.chronicle.wire.JSONWireTypesTest$Dto\":{\"field\":\"123\"}} ]}" +
                        " ]}}",
            dtoAsJson
        );

        assertEquals(dto, JSON_ONLY.fromString(dtoAsJson));
    }

    @SuppressWarnings("rawtypes")
    private Set of(Object... ts) {
        return new HashSet<>(Arrays.asList(ts));
    }

    public static class Dto extends SelfDescribingMarshallable {
        private String field;

        public String field() {
            return field;
        }

        public Dto field(String field) {
            this.field = field;
            return this;
        }
    }

    public static class DtoWithNestedSets extends SelfDescribingMarshallable {
        public Set<Set<Dto>> setOfSets;

        public Set<Set<Dto>> setOfSets() {
            return setOfSets;
        }

        public DtoWithNestedSets setOfSets(Set<Set<Dto>> setOfSets) {
            this.setOfSets = setOfSets;
            return this;
        }
    }
}
