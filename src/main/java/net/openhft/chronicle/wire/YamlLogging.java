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
    // TODO Doesn't show all writes. Use clientReads
    public static volatile boolean showServerWrites = Boolean.getBoolean("yaml.logging");
    public static volatile boolean clientWrites = Boolean.getBoolean("yaml.logging");
    @NotNull
    public static volatile String title = "";
    @NotNull
    public static volatile String writeMessage = "";
    public static volatile boolean clientReads = Boolean.getBoolean("yaml.logging");
    public static volatile boolean showServerReads = Boolean.getBoolean("yaml.logging");
    public static volatile boolean showHeartBeats = false;

    public static void setAll(boolean flag) {
        showServerReads = showServerWrites = clientWrites = clientReads = flag;
    }

    public static void setAll(YamlLoggingLevel level) {
        showServerReads = showServerWrites = clientWrites = clientReads = level.isSet();
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

