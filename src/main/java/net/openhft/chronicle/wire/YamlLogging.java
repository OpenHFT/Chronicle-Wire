/*
 * Copyright 2016 higherfrequencytrading.com
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
import org.jetbrains.annotations.NotNull;

/**
 * Class to control whether to log Yaml messages for debugging or documentation.
 */
public enum YamlLogging {
    ;
    @NotNull
    public static volatile String title = "";
    // TODO Doesn't show all writes. Use clientReads
    private static volatile boolean showServerWrites = Boolean.getBoolean("yaml.logging");
    private static volatile boolean clientWrites = Boolean.getBoolean("yaml.logging");
    @NotNull
    private static volatile String writeMessage = "";
    private static volatile boolean clientReads = Boolean.getBoolean("yaml.logging");
    private static volatile boolean showServerReads = Boolean.getBoolean("yaml.logging");
    private static volatile boolean showHeartBeats = false;

    public static void setAll(boolean flag) {
        showServerReads = showServerWrites = clientWrites = clientReads = flag;
    }

    public static void setAll(@NotNull YamlLoggingLevel level) {
        showServerReads = showServerWrites = clientWrites = clientReads = level.isSet();
    }

    public static boolean showClientReads() {
        return clientReads;
    }

    public static void writeMessage(@NotNull String s) {
        writeMessage = s;
    }

    public static void showServerWrites(boolean logging) {
        showServerWrites = logging;
    }

    public static boolean showClientWrites() {
        return clientWrites;
    }

    @NotNull
    public static String writeMessage() {
        return writeMessage;
    }

    public static boolean showHeartBeats() {
        return showHeartBeats;
    }

    public static boolean showServerReads() {
        return showServerReads;
    }

    public static void showHeartBeats(boolean log) {
        showHeartBeats = log;
    }

    public static void showClientWrites(boolean logging) {
        clientWrites = logging;
    }

    public static void showClientReads(boolean logging) {
        clientReads = logging;
    }

    public static boolean showServerWrites() {
        return showServerWrites;
    }

    public static void showServerReads(boolean logging) {
        showServerReads = logging;
    }

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

        public abstract boolean isSet();
    }
}

