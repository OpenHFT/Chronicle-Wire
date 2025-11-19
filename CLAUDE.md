# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

Chronicle-Wire is a high-performance, low-latency serialisation library providing a format-agnostic Wire API over Chronicle Bytes. It supports multiple wire formats (YAML, JSON, CSV, binary) with the same API, enabling easy format switching and schema evolution. Chronicle-Wire is part of the OpenHFT Chronicle stack and sits in Layer 1 (Data Structures & Serialisation).

**Key Features:**
- Multiple wire formats (text and binary) with a unified API
- Zero-allocation, off-heap serialisation
- Schema evolution support (field reordering, optional fields, type changes)
- MethodWriter/MethodReader for event-driven architectures
- Integration with Chronicle Queue, Map, Network, and Services

**Dependencies:**
- `chronicle-core` - Low-level utilities, resource management
- `chronicle-bytes` - Off-heap memory access
- `chronicle-threads` - Thread management
- `compiler` - Runtime Java compilation for method writers/readers

## Build Commands

### Prerequisites

- **Java 1.8** (required - later versions may cause warnings)
- Maven 3.6+

### Core Build Commands

```bash
# Clean build with tests
mvn clean verify

# Build without tests (faster iteration)
mvn clean install -DskipTests

# Run specific test class
mvn test -Dtest=ClassName

# Run specific test method
mvn test -Dtest=ClassName#methodName
```

### Code Quality Checks

```bash
# Run Checkstyle (disabled by default)
mvn checkstyle:check -Dcheckstyle.skip=false

# Run SpotBugs
mvn spotbugs:check

# Sort POM file (on demand, not automatic)
mvn sortpom:sort
```

### Performance Testing

```bash
# Run JLBH benchmarks
mvn test -Prun-benchmarks

# Run network performance tests (requires two terminals)
# Terminal 1: Start gateway
mvn exec:java@Gateway

# Terminal 2: Run latency test
mvn exec:java@EchoClient -Durl=tcp://localhost:1248

# Terminal 2: Run throughput test
mvn exec:java@EchoThroughput -Durl=tcp://localhost:1248
```

## Project Structure

```
Chronicle-Wire/
├── src/
│   ├── main/
│   │   ├── java/net/openhft/chronicle/wire/
│   │   │   ├── *.java                    # Core Wire implementation
│   │   │   ├── converter/                # LongConverter implementations
│   │   │   ├── domestic/                 # Domestic message channel support
│   │   │   ├── internal/                 # Internal utilities (not public API)
│   │   │   └── utils/                    # Utility classes
│   │   └── docs/                         # AsciiDoc documentation (canonical)
│   │       ├── architecture-overview.adoc
│   │       ├── functional-requirements.adoc
│   │       ├── wire-cookbook.adoc
│   │       └── ...
│   └── test/
│       └── java/net/openhft/chronicle/wire/  # Unit and integration tests
├── demo/                                 # Example applications
├── marshallingperf/                      # Marshalling performance benchmarks
├── microbenchmarks/                      # JMH microbenchmarks
├── docs/                                 # Additional documentation
├── AGENTS.md                             # AI agent contribution guidelines
├── README.adoc                           # User-facing documentation
└── pom.xml
```

## Architecture Overview

### Core Abstractions

**Wire API:**
- `Wire` - Main interface for serialisation/deserialisation
- `WireIn` - Reading operations
- `WireOut` - Writing operations
- `WireType` - Enum selecting format (TEXT, BINARY, JSON, YAML, CSV, RAW, etc.)

**Marshallable Pattern:**
- `Marshallable` - Interface for custom serialisation
- `SelfDescribingMarshallable` - Auto-generates `toString()`, `equals()`, `hashCode()`
- `BytesInBinaryMarshallable` - Optimised binary format without field names
- `DocumentContext` - Message framing and lifecycle management

**Method Writers/Readers:**
- `MethodWriter` - Converts method calls to wire events (bytecode generation)
- `MethodReader` - Reads wire events and invokes methods
- Used for event-driven architectures and Chronicle Queue integration

**Annotations:**
- `@FieldNumber(n)` - Assign field numbers for compact binary encoding
- `@MethodId(n)` - Assign method IDs for compact method encoding
- `@LongConversion(Converter.class)` - Custom long converters (timestamps, base64, etc.)
- `@AsMarshallable` - Force marshalling instead of pass-by-name for DynamicEnum

### Wire Format Hierarchy

```
Text Formats:          Binary Formats:
- TextWire (YAML)      - BinaryWire (self-describing)
- JSONWire             - RawWire (minimal metadata)
- CSVWire              - FieldlessBinaryWire
- YamlWire

Special:
- ReadAnyWire (auto-detect format)
```

### Key Design Patterns

- **Strategy Pattern:** `WireType` selects serialisation strategy
- **Flyweight Pattern:** Objects act as views over `Bytes` data
- **Single Writer Principle:** One writer per `Bytes` instance
- **Zero-Copy:** Direct access to off-heap memory via Chronicle Bytes
- **Dynamic Compilation:** MethodWriter/Reader generated at runtime via `compiler` module

## Common Development Tasks

### Running a Single Test

```bash
# By class name
mvn test -Dtest=MarshallableTest

# By method
mvn test -Dtest=MarshallableTest#testSimpleMarshallable

# With system property (e.g., UTC timezone required for some tests)
mvn test -Dtest=Issue327Test -Duser.timezone=UTC
```

### Adding a New Wire Format

1. Implement `Wire` interface or extend `AbstractWire`
2. Add entry to `WireType` enum
3. Implement `WireIn` and `WireOut` methods
4. Add tests in `src/test/java/.../wire/`
5. Update documentation in `src/main/docs/`

### Working with Marshallable Objects

**Basic DTO:**
```java
class MyData extends SelfDescribingMarshallable {
    String field1;
    int field2;
    // No need for toString(), equals(), hashCode()
}
```

**Custom Serialisation:**
```java
class CustomData implements Marshallable {
    @Override
    public void writeMarshallable(WireOut wire) {
        wire.write("field1").text(field1)
            .write("field2").int32(field2);
    }

    @Override
    public void readMarshallable(WireIn wire) {
        field1 = wire.read("field1").text();
        field2 = wire.read("field2").int32();
    }
}
```

### Schema Evolution

Chronicle-Wire supports:
- Adding/removing fields (use `unexpectedField()` handler)
- Changing field order
- Optional fields with defaults
- Field type changes (with caution)
- Version numbers in `BytesInBinaryMarshallable`

See `src/main/docs/wire-schema-evolution.adoc` for details.

## Testing Strategy

**Test Categories:**
- Unit tests: `*Test.java` - Verify individual components
- Integration tests: `*IT.java` - Multi-component scenarios
- Performance tests: `*JLBH.java` - Latency benchmarks using JLBH framework
- JMH benchmarks: `microbenchmarks/` - Detailed microbenchmarks

**Resource Management:**
Use `assertReferencesReleased()` from `chronicle-test-framework` to verify off-heap resources are freed:

```java
@Test
public void testExample() {
    try (Bytes<?> bytes = Bytes.allocateElasticOnHeap()) {
        // test code
    }
    // chronicle-test-framework verifies cleanup
}
```

## Documentation Standards

- **Format:** AsciiDoc (`.adoc`)
- **Location:** `src/main/docs/` (canonical) or legacy `docs/`
- **Language:** British English (e.g., "serialisation", "behaviour")
- **Encoding:** ISO-8859-1 (avoid Unicode, smart quotes)
- **Source highlighter:** `:source-highlighter: rouge`

**Key Documentation Files:**
- `architecture-overview.adoc` - High-level architecture
- `functional-requirements.adoc` - Functional requirements catalog
- `wire-cookbook.adoc` - Practical recipes
- `wire-schema-evolution.adoc` - Schema evolution guide
- `wire-faq.adoc` - Common questions
- `decision-log.adoc` - Architectural decisions

See `AGENTS.md` for detailed documentation guidelines.

## Code Style

Chronicle-Wire follows OpenHFT conventions:

- **Indentation:** 4 spaces (no tabs)
- **Brace style:** K&R (opening brace on same line)
- **Naming:**
  - Classes: `PascalCase`
  - Methods/variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
- **Javadoc:** Focus on behavior, constraints, thread-safety - not restating the obvious
- **ISO-8859-1 only:** No Unicode characters

### Checkstyle

The project uses `chronicle-baseline-checkstyle.xml`. Test sources are globally exempt.

To run manually:
```bash
mvn checkstyle:check -Dcheckstyle.skip=false \
  -Dcheckstyle.config.location=net/openhft/quality/checkstyle27/chronicle-baseline-checkstyle.xml
```

## Performance Considerations

Chronicle-Wire is designed for low-latency, zero-allocation operation:

- **Reuse objects:** Use `reset()` and object pooling to avoid allocation
- **Off-heap access:** Work directly with `Bytes` to minimise GC pressure
- **Binary formats:** Use `BinaryWire` or `RawWire` for production hot paths
- **Text formats:** Use `TextWire`/`JSONWire` for configuration and debugging
- **Method Writers:** Bytecode-generated, minimal overhead for event dispatch
- **LongConverters:** Store metadata (timestamps, IDs) efficiently in long fields

## Integration with Other Chronicle Components

**Chronicle Queue:**
- Uses `Wire` for message serialisation
- `MethodWriter` creates tailer/appender proxies
- `MethodReader` processes queue excerpts

**Chronicle Map:**
- Marshallable keys and values
- Off-heap persistence via Wire serialisation

**Chronicle Network:**
- Wire formats for TCP message protocols
- `ChronicleChannel` extends `MarshallableIn`/`MarshallableOut`

**Chronicle Services:**
- Event contracts via `MethodWriter`/`MethodReader`
- Microservice message dispatch

## Troubleshooting

### Build Failures

**Compiler warnings breaking build:**
- Ensure Java 1.8 is used (not 11, 17, 21)
- Check `JAVA_HOME` points to JDK 8

**Test failures:**
- Some tests require UTC timezone: `-Duser.timezone=UTC`
- Resource cleanup: Verify `assertReferencesReleased()` in test framework

### Dynamic Compilation Issues

MethodWriter/Reader use runtime compilation. If compilation fails:

1. Check `compiler` dependency is available
2. Verify classpath includes source interfaces
3. For Spring Boot fat JARs, extract Chronicle JARs (see README.adoc "Spring Boot" section)

### Schema Evolution Problems

- Check `unexpectedField()` handler for unknown fields
- Verify field types are compatible
- Use version numbers in `BytesInBinaryMarshallable` for breaking changes

## Important Notes

1. **Java Version:** Always use Java 1.8 for builds to avoid compiler warnings
2. **Off-heap Resources:** Must be explicitly released - use try-with-resources and test framework assertions
3. **Thread Safety:** Wire instances are not thread-safe - use one per thread or synchronise access
4. **Performance Testing:** Use JLBH for realistic latency measurements (not JMH for latency)
5. **Binary Compatibility:** Check `binary-compatibility-enforcer-plugin` before releases

## Additional Resources

- [README.adoc](README.adoc) - User guide with examples
- [AGENTS.md](AGENTS.md) - Contribution guidelines for AI agents and humans
- [Documentation Index](src/main/docs/index.adoc) - Complete documentation catalog
- [TODO.md](TODO.md) - Development roadmap and architecture work
- GitHub Issues: https://github.com/OpenHFT/Chronicle-Wire/issues
- Chronicle Software: https://chronicle.software/
