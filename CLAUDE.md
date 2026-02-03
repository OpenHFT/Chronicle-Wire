# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Chronicle Wire is a high-performance, zero-GC serialisation library built on top of the Chronicle stack. It abstracts underlying wire formats (YAML, JSON, Binary), allowing human-readable text or compact binary interchangeably through a common API. Typical use cases include configuration files, persistence, and low-latency IPC between JVMs.

## Build and Test Commands

### Standard Commands
```bash
# Create logs directory first
mkdir -p logs

# Full verification (preferred)
mvn verify -l logs/mvn-verify.log

# Clean build when needed
mvn clean verify -l logs/mvn-clean-verify.log

# Run a single test class
mvn -Dtest=ClassName test -l logs/mvn-test.log

# Run a specific test method
mvn -Dtest=ClassName#methodName test -l logs/mvn-test.log
```

### Reviewing Build Output
```bash
# Check for warnings and errors in logs
rg -n '^\[(WARNING|ERROR)\]|SLF4J\(W\)|\bWARNING:|\bwarning:' logs/mvn-verify.log
```

**Important:** Do not commit the `logs/` directory.

## Code Constraints

- **Java 8 baseline** - avoid newer language features
- **ISO-8859-1 encoding** - source files must stay in ISO-8859-1 (code points 0-255); prefer ASCII; avoid smart quotes and non-breaking spaces
- **API stability** - preserve public APIs unless explicitly requested
- **Treat warnings as defects** - keep build logs clean of warnings

## Architecture

### Core Components

Chronicle Wire operates in layers:

1. **Application Layer** - POJOs implementing `Marshallable` or `SelfDescribingMarshallable`
2. **Wire API Layer** - `Wire`, `WireIn`/`WireOut`, `ValueIn`/`ValueOut`, `DocumentContext`, `WireType`
3. **Format Implementation Layer** - `YamlWire`, `JSONWire`, `BinaryWire`, `TextWire`, `RawWire`
4. **Bytes Layer** - `chronicle-bytes` library for low-level byte manipulation (on-heap, off-heap, memory-mapped)

### Key Abstractions

- **`Wire`** - Central interface for reading/writing structured data
- **`WireType`** - Enum factory for wire formats (YAML, JSON, BINARY, BINARY_LIGHT, FIELDLESS_BINARY, RAW, TEXT, CSV, READ_ANY)
- **`Marshallable`** / **`SelfDescribingMarshallable`** - Interfaces for auto-serialization of POJOs
- **`BytesInBinaryMarshallable`** - For performance-critical raw binary serialization
- **`DocumentContext`** - Manages message boundaries in streaming scenarios
- **`LongConverter`** - Converts long values to/from string representations (timestamps, Base64/85 encoded strings)

### Wire Format Trade-offs

| Format | Human Readable | Self-Describing | Performance |
|--------|---------------|-----------------|-------------|
| YAML/JSON/TEXT | Yes | Yes | Lower |
| BINARY_LIGHT | No | Yes | Higher |
| FIELDLESS_BINARY | No | No | Highest (schema-sensitive) |
| RAW | No | No | For BytesMarshallable |

### Schema Evolution

Self-describing formats (YAML, JSON, BINARY) support:
- Adding/removing fields
- Reordering fields
- Field renaming via `@FieldNumber` annotation

FIELDLESS_BINARY requires exact schema match between reader and writer.

## Key Packages

- `net.openhft.chronicle.wire` - Core wire interfaces and implementations
- Wire implementations: `BinaryWire`, `YamlWire`, `JSONWire`, `TextWire`, `RawWire`, `CSVWire`
- Converters: `*LongConverter` classes for timestamp/encoding conversions
- Method writers/readers: `GenerateMethodReader`, `GenerateMethodWriter2` for dynamic proxy generation

## Dependencies

Core Chronicle components:
- `chronicle-core` - Low-level utilities and unsafe operations
- `chronicle-bytes` - Off-heap byte buffer abstraction
- `chronicle-threads` - Threading utilities
- `compiler` - Runtime code generation

## Documentation

- AsciiDoc files in `src/main/docs/` - keep in sync with code changes
- Javadoc should document behavioral contracts, edge cases, thread safety, and performance notes
- Reference docs: `src/main/docs/decision-log.adoc`, `src/main/docs/project-requirements.adoc`
