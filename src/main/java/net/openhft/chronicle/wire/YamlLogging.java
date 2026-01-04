/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
    /**
     * Optional title prefix used when emitting YAML logs.
     */
    @NotNull
    @Deprecated(/* to be removed in 2027 */)
    @SuppressFBWarnings(value = "MS_PKGPROTECT", justification = "Public title prefix kept for compatibility until removal.")
    public static String title = "";

    /**
     * Updates the title prefix used in YAML logs.
     *
     * @param newTitle the new title prefix, or an empty string to clear it
     */
    @Deprecated(/* to be removed in 2027 */)
    public static void setTitle(@NotNull String newTitle) {
        title = newTitle;
    }

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
    @Deprecated(/* to be removed in 2027 */)
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
     * @param message The new message to be set.
     */
    @Deprecated(/* to be removed in 2027 */)
    public static void writeMessage(@NotNull String message) {
        writeMessage = message;
    }

    /**
     * Sets the flag to determine whether server writes should be logged.
     *
     * @param flag {@code true} to enable logging for server writes; {@code false} to disable.
     */
    @Deprecated(/* to be removed in 2027 */)
    public static void showServerWrites(boolean flag) {
        showServerWrites = flag;
    }

    /**
     * Checks whether logging for client writes is enabled.
     *
     * @return {@code true} if logging for client writes is enabled; {@code false} otherwise.
     */
    @Deprecated(/* to be removed in 2027 */)
    public static boolean showClientWrites() {
        return clientWrites;
    }

    /**
     * Retrieves the current message associated with a write operation.
     *
     * @return The message currently associated with a write operation.
     */
    @NotNull
    @Deprecated(/* to be removed in 2027 */)
    public static String writeMessage() {
        return writeMessage;
    }

    /**
     * Checks whether heartbeat logging is enabled.
     *
     * @return {@code true} if heartbeat logging is enabled; {@code false} otherwise.
     */
    @Deprecated(/* to be removed in 2027 */)
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
     * @param flag {@code true} to enable heartbeat logging; {@code false} to disable.
     */
    @Deprecated(/* to be removed in 2027 */)
    public static void showHeartBeats(boolean flag) {
        showHeartBeats = flag;
    }

    /**
     * Sets the flag to determine whether client writes should be logged.
     *
     * @param flag {@code true} to enable logging for client writes; {@code false} to disable.
     */
    @Deprecated(/* to be removed in 2027 */)
    public static void showClientWrites(boolean flag) {
        clientWrites = flag;
    }

    /**
     * Sets the flag to determine whether client reads should be logged.
     *
     * @param flag {@code true} to enable logging for client reads; {@code false} to disable.
     */
    @Deprecated(/* to be removed in 2027 */)
    public static void showClientReads(boolean flag) {
        clientReads = flag;
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
     * @param flag {@code true} to enable logging for server reads; {@code false} to disable.
     */
    @Deprecated(/* to be removed in 2027 */)
    public static void showServerReads(boolean flag) {
        showServerReads = flag;
    }

    /**
     * Enum representing the various logging levels for Yaml.
     * The levels include OFF (no logging), DEBUG_ONLY (logs only when in debug mode), and ON (always logs).
     */
    @Deprecated(/* to be removed in 2027 */)
    public enum YamlLoggingLevel {
        /** Logging is disabled for all YAML operations. */
        OFF {
            @Override
            public boolean isSet() {
                return false;
            }
        },
        /** Logging is enabled only when debugging is active. */
        DEBUG_ONLY {
            @Override
            public boolean isSet() {
                return Jvm.isDebug();
            }
        },
        /** Logging is always enabled for all YAML operations. */
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
