package net.openhft.chronicle.wire;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static net.openhft.chronicle.wire.WireType.JSON_ONLY;
import static org.junit.Assert.assertEquals;

public class JSONWireTypesTest extends WireTestCommon {
    @Test
    public void nestedSets() {
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

    private <T> Set<T> of(T... ts) {
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
        private Set<Set<Dto>> setOfSets;

        public Set<Set<Dto>> setOfSets() {
            return setOfSets;
        }

        public DtoWithNestedSets setOfSets(Set<Set<Dto>> setOfSets) {
            this.setOfSets = setOfSets;
            return this;
        }
    }
}
