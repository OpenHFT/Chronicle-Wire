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
 * Provides utility functions to control the logging of Yaml messages, which can be
 * useful for debugging or generating documentation. The logging settings can be toggled
 * using system properties or programmatically through this class's static methods.
 */
// Used in Chronicle Services
public enum YamlLogging {
    ; // No enum instances are intended for this utility enum.

    // The title for logging (can be changed during runtime).
    @NotNull
    public static volatile String title = "";

    // Flag indicating whether server writes should be shown.
    // TODO Doesn't show all writes. Use clientReads instead.
    private static volatile boolean showServerWrites = Jvm.getBoolean("yaml.logging");

    // Flag indicating whether client writes should be shown.
    private static volatile boolean clientWrites = Jvm.getBoolean("yaml.logging");

    // A message associated with a write operation.
    @NotNull
    private static volatile String writeMessage = "";

    // Flag indicating whether client reads should be shown.
    private static volatile boolean clientReads = Jvm.getBoolean("yaml.logging");

    // Flag indicating whether server reads should be shown.
    private static volatile boolean showServerReads = Jvm.getBoolean("yaml.logging");

    // Flag indicating whether heartbeat messages should be shown.
    private static volatile boolean showHeartBeats = false;

    /**
     * Sets the logging flags for all message types (reads/writes for both client and server).
     *
     * @param flag The boolean value to set for all logging flags.
     */
    public static void setAll(boolean flag) {
        showServerReads = showServerWrites = clientWrites = clientReads = flag;
    }

    /**
     * Sets the logging flags for all message types based on the provided {@link YamlLoggingLevel}.
     *
     * @param level The {@link YamlLoggingLevel} determining whether to set or unset the logging flags.
     */
    public static void setAll(@NotNull YamlLoggingLevel level) {
        showServerReads = showServerWrites = clientWrites = clientReads = level.isSet();
    }

    /**
     * Checks whether logging for client reads is enabled.
     *
     * @return {@code true} if logging for client reads is enabled; {@code false} otherwise.
     */
    public static boolean showClientReads() {
        return clientReads;
    }

    /**
     * Updates the message associated with a write operation.
     *
     * @param s The new message to be set.
     */
    public static void writeMessage(@NotNull String s) {
        writeMessage = s;
    }

    /**
     * Sets the flag to determine whether server writes should be logged.
     *
     * @param logging {@code true} to enable logging for server writes; {@code false} to disable.
     */
    public static void showServerWrites(boolean logging) {
        showServerWrites = logging;
    }

    /**
     * Checks whether logging for client writes is enabled.
     *
     * @return {@code true} if logging for client writes is enabled; {@code false} otherwise.
     */
    public static boolean showClientWrites() {
        return clientWrites;
    }

    /**
     * Retrieves the current message associated with a write operation.
     *
     * @return The message currently associated with a write operation.
     */
    @NotNull
    public static String writeMessage() {
        return writeMessage;
    }

    /**
     * Checks whether heartbeat logging is enabled.
     *
     * @return {@code true} if heartbeat logging is enabled; {@code false} otherwise.
     */
    public static boolean showHeartBeats() {
        return showHeartBeats;
    }

    /**
     * Checks whether logging for server reads is enabled.
     *
     * @return {@code true} if logging for server reads is enabled; {@code false} otherwise.
     */
    public static boolean showServerReads() {
        return showServerReads;
    }

    /**
     * Sets the flag to determine whether heartbeats should be logged.
     *
     * @param log {@code true} to enable heartbeat logging; {@code false} to disable.
     */
    public static void showHeartBeats(boolean log) {
        showHeartBeats = log;
    }

    /**
     * Sets the flag to determine whether client writes should be logged.
     *
     * @param logging {@code true} to enable logging for client writes; {@code false} to disable.
     */
    public static void showClientWrites(boolean logging) {
        clientWrites = logging;
    }

    /**
     * Sets the flag to determine whether client reads should be logged.
     *
     * @param logging {@code true} to enable logging for client reads; {@code false} to disable.
     */
    public static void showClientReads(boolean logging) {
        clientReads = logging;
    }

    /**
     * Checks whether logging for server writes is enabled.
     *
     * @return {@code true} if logging for server writes is enabled; {@code false} otherwise.
     */
    public static boolean showServerWrites() {
        return showServerWrites;
    }

    /**
     * Sets the flag to determine whether server reads should be logged.
     *
     * @param logging {@code true} to enable logging for server reads; {@code false} to disable.
     */
    public static void showServerReads(boolean logging) {
        showServerReads = logging;
    }

    /**
     * Enum representing the various logging levels for Yaml.
     * The levels include OFF (no logging), DEBUG_ONLY (logs only when in debug mode), and ON (always logs).
     */
    public enum YamlLoggingLevel {
        OFF {
            @Override
            public boolean isSet() {
                return false;
            }
        },
        DEBUG_ONLY {
            @Override
            public boolean isSet() {
                return Jvm.isDebug();
            }
        },
        ON {
            @Override
            public boolean isSet() {
                return true;
            }
        };

        /**
         * Checks if the current logging level is set (enabled).
         *
         * @return {@code true} if the current logging level is enabled; {@code false} otherwise.
         */
        public abstract boolean isSet();
    }
}
