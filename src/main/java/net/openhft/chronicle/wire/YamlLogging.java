/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

