/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Examples of YAML anchor usage in Chronicle Wire.
 * These examples are referenced in the README.adoc documentation.
 */
class YamlAnchorExamplesTest extends WireTestCommon {

    // tag::database-config-classes[]
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD", "NP_UNWRITTEN_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class DatabaseConfig extends SelfDescribingMarshallable {
        String host;
        int port;
        String username;
    }

    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD", "NP_UNWRITTEN_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class CacheConfig extends SelfDescribingMarshallable {
        String host;
        int port;
        int timeout;
    }

    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD", "NP_UNWRITTEN_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class BackupConfig extends SelfDescribingMarshallable {
        String host;
        int port;
        String schedule;
    }

    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD", "NP_UNWRITTEN_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class SystemConfig extends SelfDescribingMarshallable {
        DatabaseConfig database;
        CacheConfig cache;
        BackupConfig backup;
    }
    // end::database-config-classes[]

    // tag::server-config-classes[]
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD", "NP_UNWRITTEN_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class ServerConfig extends SelfDescribingMarshallable {
        int timeout;
        int retries;
        String logLevel;
    }

    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD", "NP_UNWRITTEN_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class MonitorConfig extends SelfDescribingMarshallable {
        ServerConfig server;
        int interval;
    }

    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD", "NP_UNWRITTEN_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class ServerSystemConfig extends SelfDescribingMarshallable {
        ServerConfig defaults;
        ServerConfig primary;
        ServerConfig secondary;
        MonitorConfig monitoring;
    }
    // end::server-config-classes[]

    @BeforeEach
    void hasDirect() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for YAML anchor examples");
    }

    @Test
    @DisplayName("YAML field anchors reuse scalar host values")
    @SuppressFBWarnings(
            value = "NP_UNWRITTEN_FIELD",
            justification = "Wire marshalling populates fields without explicit setters.")
    void testBasicYamlFieldAnchors() {
        // tag::basic-yaml-example[]
        String yaml = "database: {\n" +
                "  host: &dbHost \"production.example.com\",\n" +
                "  port: 5432,\n" +
                "  username: admin\n" +
                "}\n" +
                "cache: {\n" +
                "  host: *dbHost,\n" +
                "  port: 6379,\n" +
                "  timeout: 30\n" +
                "}\n" +
                "backup: {\n" +
                "  host: *dbHost,\n" +
                "  port: 5432,\n" +
                "  schedule: \"0 2 * * *\"\n" +
                "}\n";

        // Deserialize directly to the SystemConfig DTO
        SystemConfig systemConfig = WireType.YAML.fromString(SystemConfig.class, yaml);

        assertEquals("production.example.com",
                systemConfig.database.host,
                "Database host should match the production example");
        assertSame(systemConfig.cache.host,
                systemConfig.database.host,
                "Cache host should share the database host anchor");
        assertSame(systemConfig.backup.host,
                systemConfig.database.host,
                "Backup host should share the database host anchor");
        // end::basic-yaml-example[]

        assertEquals(5432,
                systemConfig.database.port,
                "Database port should match the expected value");
        assertEquals(5432,
                systemConfig.backup.port,
                "Backup port should match the database port");
    }

    @Test
    @DisplayName("YAML object anchors reuse server configuration instances")
    @SuppressFBWarnings(
            value = "NP_UNWRITTEN_FIELD",
            justification = "Wire marshalling populates fields without explicit setters.")
    void testObjectAnchors() {
        // tag::object-anchor-example[]
        String yaml = "defaults: &defaultServer !net.openhft.chronicle.wire.YamlAnchorExamplesTest$ServerConfig {\n" +
                "  timeout: 30,\n" +
                "  retries: 3,\n" +
                "  logLevel: INFO\n" +
                "}\n" +
                "primary: *defaultServer\n" +
                "secondary: *defaultServer\n" +
                "monitoring: {\n" +
                "  server: *defaultServer,\n" +
                "  interval: 60\n" +
                "}\n";

        // Deserialize directly to the top-level object
        ServerSystemConfig config = WireType.YAML.fromString(ServerSystemConfig.class, yaml);

        // Verify object references work - they should be the same object instance
        assertSame(config.defaults, config.primary, "primary should be same object as defaults");
        assertSame(config.defaults, config.secondary, "secondary should be same object as defaults");
        assertSame(config.defaults, config.monitoring.server, "monitoring.server should be same object as defaults");

        // Verify the values
        assertEquals(30, config.defaults.timeout, "Default timeout should match YAML example");
        assertEquals(3, config.defaults.retries, "Default retries should match YAML example");
        assertEquals("INFO", config.defaults.logLevel, "Default log level should match YAML");
        assertEquals(60, config.monitoring.interval, "Monitoring interval should match YAML example");
        // end::object-anchor-example[]
    }
}
