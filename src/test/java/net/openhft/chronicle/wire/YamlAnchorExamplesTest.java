/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Examples of YAML anchor usage in Chronicle Wire.
 * These examples are referenced in the README.adoc documentation.
 */
class YamlAnchorExamplesTest extends WireTestCommon {

    // tag::database-config-classes[]
    static class DatabaseConfig extends SelfDescribingMarshallable {
        String host;
        int port;
        String username;
    }

    static class CacheConfig extends SelfDescribingMarshallable {
        String host;
        int port;
        int timeout;
    }

    static class BackupConfig extends SelfDescribingMarshallable {
        String host;
        int port;
        String schedule;
    }

    static class SystemConfig extends SelfDescribingMarshallable {
        DatabaseConfig database;
        CacheConfig cache;
        BackupConfig backup;
    }
    // end::database-config-classes[]

    // tag::server-config-classes[]
    static class ServerConfig extends SelfDescribingMarshallable {
        int timeout;
        int retries;
        String logLevel;
    }

    static class MonitorConfig extends SelfDescribingMarshallable {
        ServerConfig server;
        int interval;
    }

    static class ServerSystemConfig extends SelfDescribingMarshallable {
        ServerConfig defaults;
        ServerConfig primary;
        ServerConfig secondary;
        MonitorConfig monitoring;
    }
    // end::server-config-classes[]

    @BeforeEach
    void hasDirect() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
    }

    @Test
    void testBasicYamlFieldAnchors() {
        // tag::basic-yaml-example[]
        String yaml = "" +
                "database: {\n" +
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

        assertEquals("production.example.com", systemConfig.database.host);
        assertSame(systemConfig.cache.host, systemConfig.database.host);
        assertSame(systemConfig.backup.host, systemConfig.database.host);
        // end::basic-yaml-example[]

        assertEquals(5432, systemConfig.database.port);
        assertEquals(5432, systemConfig.backup.port);
    }

    @Test
    void testObjectAnchors() {
        // tag::object-anchor-example[]
        String yaml = "" +
                "defaults: &defaultServer !net.openhft.chronicle.wire.YamlAnchorExamplesTest$ServerConfig {\n" +
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
        assertEquals(30, config.defaults.timeout);
        assertEquals(3, config.defaults.retries);
        assertEquals("INFO", config.defaults.logLevel);
        assertEquals(60, config.monitoring.interval);
        // end::object-anchor-example[]
    }
}
