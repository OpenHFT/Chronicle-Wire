/*
 * Copyright 2016-2020 chronicle.software
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

import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;

/**
 * Utility to control YAML message logging.
 * <p>
 * The behaviour can be toggled via system properties or at runtime through this
 * class's static methods, which aids debugging and documentation generation.
 */
// Used in Chronicle Services
public enum YamlLogging {
    ; // No enum instances are intended for this utility enum.

    /**
     * Global title prepended to YAML messages. May be changed at runtime.
     */
    @NotNull
    public static volatile String title = "";

    /**
     * Whether server write operations are logged as YAML. Initialised from the
     * {@code yaml.logging} system property. Modifiable at runtime.
     * TODO Doesn't show all writes. Use clientReads instead.
     */
    private static volatile boolean showServerWrites = Jvm.getBoolean("yaml.logging");

    /**
     * Whether client write operations are logged as YAML. Initialised from the
     * {@code yaml.logging} system property. Modifiable at runtime.
     */
    private static volatile boolean clientWrites = Jvm.getBoolean("yaml.logging");

    /**
     * Message associated with write operations that may appear in logs.
     */
    @NotNull
    private static volatile String writeMessage = "";

    /**
     * Whether client read operations are logged as YAML. Initialised from the
     * {@code yaml.logging} system property. Modifiable at runtime.
     */
    private static volatile boolean clientReads = Jvm.getBoolean("yaml.logging");

    /**
     * Whether server read operations are logged as YAML. Initialised from the
     * {@code yaml.logging} system property. Modifiable at runtime.
     */
    private static volatile boolean showServerReads = Jvm.getBoolean("yaml.logging");

    /**
     * Whether heartbeat messages are logged as YAML. Defaults to {@code false}.
     * Modifiable at runtime.
     */
    private static volatile boolean showHeartBeats = false;

    /**
     * Enable or disable logging for all message types.
     *
     * @param flag {@code true} to enable, {@code false} to disable.
     */
    public static void setAll(boolean flag) {
        showServerReads = showServerWrites = clientWrites = clientReads = flag;
    }

    /**
     * Set logging flags for all message types according to the level.
     */
    public static void setAll(@NotNull YamlLoggingLevel level) {
        showServerReads = showServerWrites = clientWrites = clientReads = level.isSet();
    }

    /**
     * Returns {@code true} if client read logging is enabled.
     */
    public static boolean showClientReads() {
        return clientReads;
    }

    /**
     * Associate a message with subsequent write operations.
     */
    public static void writeMessage(@NotNull String s) {
        writeMessage = s;
    }

    /**
     * Enable or disable logging of server writes.
     */
    public static void showServerWrites(boolean logging) {
        showServerWrites = logging;
    }

    /**
     * Returns {@code true} if client write logging is enabled.
     */
    public static boolean showClientWrites() {
        return clientWrites;
    }

    /**
     * Returns the message associated with write operations.
     */
    @NotNull
    public static String writeMessage() {
        return writeMessage;
    }

    /**
     * Returns {@code true} if heartbeat logging is enabled.
     */
    public static boolean showHeartBeats() {
        return showHeartBeats;
    }

    /**
     * Returns {@code true} if server read logging is enabled.
     */
    public static boolean showServerReads() {
        return showServerReads;
    }

    /**
     * Enable or disable logging of heartbeats.
     */
    public static void showHeartBeats(boolean log) {
        showHeartBeats = log;
    }

    /**
     * Enable or disable logging of client writes.
     */
    public static void showClientWrites(boolean logging) {
        clientWrites = logging;
    }

    /**
     * Enable or disable logging of client reads.
     */
    public static void showClientReads(boolean logging) {
        clientReads = logging;
    }

    /**
     * Returns {@code true} if server write logging is enabled.
     */
    public static boolean showServerWrites() {
        return showServerWrites;
    }

    /**
     * Enable or disable logging of server reads.
     */
    public static void showServerReads(boolean logging) {
        showServerReads = logging;
    }

    /**
     * Levels controlling when YAML logging is active.
     */
    public enum YamlLoggingLevel {
        /** Logging is disabled. */
        OFF {
            @Override
            public boolean isSet() {
                return false;
            }
        },
        /** Logging enabled only when {@link Jvm#isDebug()} is {@code true}. */
        DEBUG_ONLY {
            @Override
            public boolean isSet() {
                return Jvm.isDebug();
            }
        },
        /** Logging is always enabled. */
        ON {
            @Override
            public boolean isSet() {
                return true;
            }
        };

        /**
         * Returns {@code true} if this level currently enables logging.
         */
        public abstract boolean isSet();
    }
}
