/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for schema evolution and backward/forward compatibility.
 * These scenarios are critical for production systems where data formats evolve over time.
 */
@SuppressWarnings({"deprecation", "removal"})
class WireSchemaEvolutionTest extends WireTestCommon {

    // ========== Version 1 Classes ==========

    public static class UserV1 extends SelfDescribingMarshallable {
        public String name;

        public UserV1() {
        }

        public UserV1(String name) {
            this.name = name;
        }
    }

    // ========== Version 2 Classes (Added Fields) ==========

    public static class UserV2 extends SelfDescribingMarshallable {
        public String name;
        public int age;
        public String email;

        public UserV2() {
        }

        public UserV2(String name, int age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }
    }

    // ========== Type Widening Classes ==========

    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD", "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    public static class NumericV1Short extends SelfDescribingMarshallable {
        public short value;

        public NumericV1Short() {
        }

        public NumericV1Short(short value) {
            this.value = value;
        }
    }

    public static class NumericV2Long extends SelfDescribingMarshallable {
        public long value;

        public NumericV2Long() {
        }

        public NumericV2Long(long value) {
            this.value = value;
        }
    }

    // ========== Collection Evolution Classes ==========

    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD", "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    public static class TagsV1 extends SelfDescribingMarshallable {
        public String tag;

        public TagsV1() {
        }

        public TagsV1(String tag) {
            this.tag = tag;
        }
    }

    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD", "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    public static class TagsV2 extends SelfDescribingMarshallable {
        public List<String> tags;

        public TagsV2() {
            this.tags = new ArrayList<>();
        }

        public TagsV2(List<String> tags) {
            this.tags = new ArrayList<>(tags);
        }
    }

    // ========== Forward Compatibility Tests (Read newer data with older class) ==========

    @Test
    @DisplayName("Reading V2 data with V1 class should skip extra fields (BinaryWire)")
    void testForwardCompatibilityBinary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        // Write as V2 (more fields)
        UserV2 v2 = new UserV2("Alice", 30, "alice@example.com");
        wire.write("user").object(v2);

        bytes.readPosition(0);

        // Read as V1 (fewer fields) - should ignore extra fields
        UserV1 v1 = wire.read("user").object(UserV1.class);
        assertNotNull(v1, "V1 object should be created");
        assertEquals("Alice", v1.name, "Binary V1 name should be read from V2 data");
    }

    @Test
    @DisplayName("Reading V2 data with V1 class should skip extra fields (TextWire)")
    void testForwardCompatibilityText() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        TextWire wire = new TextWire(bytes);

        UserV2 v2 = new UserV2("Bob", 25, "bob@example.com");
        wire.write("user").object(v2);

        bytes.readPosition(0);

        UserV1 v1 = wire.read("user").object(UserV1.class);
        assertNotNull(v1, "V1 object should be created from TextWire");
        assertEquals("Bob", v1.name, "Name should be read correctly from TextWire");
    }

    @Test
    @DisplayName("Reading V2 data with V1 class should skip extra fields (YamlWire)")
    void testForwardCompatibilityYaml() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        YamlWire wire = new YamlWire(bytes);

        UserV2 v2 = new UserV2("Carol", 35, "carol@example.com");
        wire.write("user").object(v2);

        bytes.readPosition(0);

        UserV1 v1 = wire.read("user").object(UserV1.class);
        assertNotNull(v1, "V1 object should be created from YamlWire");
        assertEquals("Carol", v1.name, "Name should be read correctly from YamlWire");
    }

    // ========== Backward Compatibility Tests (Read older data with newer class) ==========

    @Test
    @DisplayName("Reading V1 data with V2 class should use defaults for missing fields (BinaryWire)")
    void testBackwardCompatibilityBinary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        // Write as V1 (fewer fields)
        UserV1 v1 = new UserV1("Dave");
        wire.write("user").object(v1);

        bytes.readPosition(0);

        // Read as V2 (more fields) - missing fields should get defaults
        UserV2 v2 = wire.read("user").object(UserV2.class);
        assertNotNull(v2, "V2 object should be created");
        assertEquals("Dave", v2.name, "Binary V2 name should read from V1 data");
        assertEquals(0, v2.age, "Binary V2 age should default to 0 when missing");
        assertNull(v2.email, "Binary V2 email should default to null when missing");
    }

    @Test
    @DisplayName("Reading V1 data with V2 class should use defaults for missing fields (TextWire)")
    void testBackwardCompatibilityText() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        TextWire wire = new TextWire(bytes);

        UserV1 v1 = new UserV1("Eve");
        wire.write("user").object(v1);

        bytes.readPosition(0);

        UserV2 v2 = wire.read("user").object(UserV2.class);
        assertNotNull(v2, "V2 object should be created from TextWire");
        assertEquals("Eve", v2.name, "Text wire V2 name should read from V1 data");
        assertEquals(0, v2.age, "Text wire V2 age should default to 0 when missing");
        assertNull(v2.email, "Text wire V2 email should default to null when missing");
    }

    @Test
    @DisplayName("Reading V1 data with V2 class should use defaults for missing fields (YamlWire)")
    void testBackwardCompatibilityYaml() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        YamlWire wire = new YamlWire(bytes);

        UserV1 v1 = new UserV1("Frank");
        wire.write("user").object(v1);

        bytes.readPosition(0);

        UserV2 v2 = wire.read("user").object(UserV2.class);
        assertNotNull(v2, "V2 object should be created from YamlWire");
        assertEquals("Frank", v2.name, "Yaml wire V2 name should read from V1 data");
        assertEquals(0, v2.age, "Yaml wire V2 age should default to 0 when missing");
        assertNull(v2.email, "Yaml wire V2 email should default to null when missing");
    }

    // ========== Type Widening Tests ==========

    @Test
    @DisplayName("Short value should be readable as long (type widening)")
    void testTypeWideningShortToLong() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            // Write as short
            NumericV1Short v1 = new NumericV1Short((short) 12345);
            wire.write("num").object(v1);

            bytes.readPosition(0);

            // Read as long
            NumericV2Long v2 = wire.read("num").object(NumericV2Long.class);
            assertNotNull(v2, "V2 object should be created in " + wt);
            assertEquals(12345L, v2.value, "Value should widen correctly in " + wt);
        }
    }

    // ========== Reordered Fields Tests ==========

    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD", "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    public static class OrderedV1 extends SelfDescribingMarshallable {
        public String first;
        public String second;

        public OrderedV1() {
        }

        public OrderedV1(String first, String second) {
            this.first = first;
            this.second = second;
        }
    }

    public static class OrderedV2 extends SelfDescribingMarshallable {
        public String second;  // Reordered
        public String first;

        public OrderedV2() {
        }

        public OrderedV2(String second, String first) {
            this.second = second;
            this.first = first;
        }
    }

    @Test
    @DisplayName("Field order should not matter for self-describing wire")
    void testReorderedFields() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            OrderedV1 v1 = new OrderedV1("A", "B");
            wire.write("obj").object(v1);

            bytes.readPosition(0);

            OrderedV2 v2 = wire.read("obj").object(OrderedV2.class);
            assertNotNull(v2, "V2 with reordered fields should be created in " + wt);
            assertEquals("A", v2.first, "First should match in " + wt);
            assertEquals("B", v2.second, "Second should match in " + wt);
        }
    }

    // ========== Renamed Field Handling ==========

    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD", "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    public static class RenamedV1 extends SelfDescribingMarshallable {
        public String oldName;

        public RenamedV1() {
        }

        public RenamedV1(String oldName) {
            this.oldName = oldName;
        }
    }

    public static class RenamedV2 extends SelfDescribingMarshallable {
        public String newName;

        public RenamedV2() {
        }

        public RenamedV2(String newName) {
            this.newName = newName;
        }
    }

    @Test
    @DisplayName("Renamed field mapping should keep the new field null")
    void testRenamedFields() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            RenamedV1 v1 = new RenamedV1("value");
            wire.write("obj").object(v1);

            bytes.readPosition(0);

            // Reading with renamed field - oldName won't map to newName
            RenamedV2 v2 = wire.read("obj").object(RenamedV2.class);
            assertNotNull(v2, "V2 should be created in " + wt);
            assertNull(v2.newName, "Renamed field should remain null because old name is missing in " + wt);
        }
    }

    // ========== Nested Object Evolution ==========

    public static class OuterV1 extends SelfDescribingMarshallable {
        public UserV1 user;

        public OuterV1() {
        }

        public OuterV1(UserV1 user) {
            this.user = user;
        }
    }

    public static class OuterV2 extends SelfDescribingMarshallable {
        public UserV2 user;

        public OuterV2() {
        }

        public OuterV2(UserV2 user) {
            this.user = user;
        }
    }

    @Test
    @DisplayName("Nested objects should handle forward compatibility")
    void testNestedForwardCompatibility() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            // Write with nested V2
            OuterV2 outer2 = new OuterV2(new UserV2("Grace", 40, "grace@example.com"));
            wire.write("outer").object(outer2);

            bytes.readPosition(0);

            // Read as V1 with nested V1
            OuterV1 outer1 = wire.read("outer").object(OuterV1.class);
            assertNotNull(outer1, "Outer V1 should be created in " + wt);
            assertNotNull(outer1.user, "Forward nested user should be created in " + wt);
            assertEquals("Grace", outer1.user.name, "Forward nested name should match in " + wt);
        }
    }

    @Test
    @DisplayName("Nested objects should handle backward compatibility")
    void testNestedBackwardCompatibility() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            // Write with nested V1
            OuterV1 outer1 = new OuterV1(new UserV1("Henry"));
            wire.write("outer").object(outer1);

            bytes.readPosition(0);

            // Read as V2 with nested V2
            OuterV2 outer2 = wire.read("outer").object(OuterV2.class);
            assertNotNull(outer2, "Outer V2 should be created in " + wt);
            assertNotNull(outer2.user, "Backward nested user should be created in " + wt);
            assertEquals("Henry", outer2.user.name, "Backward nested name should match in " + wt);
            assertEquals(0, outer2.user.age, "Backward nested age should default to 0 in " + wt);
            assertNull(outer2.user.email, "Backward nested email should be null in " + wt);
        }
    }

    // ========== Empty Object Evolution ==========

    public static class EmptyV1 extends SelfDescribingMarshallable {
        public EmptyV1() {
        }
    }

    public static class EmptyV2 extends SelfDescribingMarshallable {
        public String newField;

        public EmptyV2() {
        }

        public EmptyV2(String newField) {
            this.newField = newField;
        }
    }

    @Test
    @DisplayName("Empty object V1 should read as V2 with defaults")
    void testEmptyToNonEmpty() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            EmptyV1 v1 = new EmptyV1();
            wire.write("obj").object(v1);

            bytes.readPosition(0);

            EmptyV2 v2 = wire.read("obj").object(EmptyV2.class);
            assertNotNull(v2, "V2 should be created from empty V1 in " + wt);
            assertNull(v2.newField, "New field should be null in " + wt);
        }
    }

    @Test
    @DisplayName("Non-empty V2 object should read as empty V1 schema")
    void testNonEmptyToEmpty() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            EmptyV2 v2 = new EmptyV2("value");
            wire.write("obj").object(v2);

            bytes.readPosition(0);

            EmptyV1 v1 = wire.read("obj").object(EmptyV1.class);
            assertNotNull(v1, "V1 should be created from non-empty V2 data in " + wt);
        }
    }
}
