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
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assume.assumeFalse;

/**
 * Examples of YAML anchor usage in Chronicle Wire.
 * These examples are referenced in the README.adoc documentation.
 */
public class YamlAnchorExamplesTest extends WireTestCommon {

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

    @Before
    public void hasDirect() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
    }

    @Test
    public void testBasicYamlFieldAnchors() {
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
    public void testObjectAnchors() {
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
        assertSame("primary should be same object as defaults", config.defaults, config.primary);
        assertSame("secondary should be same object as defaults", config.defaults, config.secondary);
        assertSame("monitoring.server should be same object as defaults", config.defaults, config.monitoring.server);

        // Verify the values
        assertEquals(30, config.defaults.timeout);
        assertEquals(3, config.defaults.retries);
        assertEquals("INFO", config.defaults.logLevel);
        assertEquals(60, config.monitoring.interval);
        // end::object-anchor-example[]
    }
}
