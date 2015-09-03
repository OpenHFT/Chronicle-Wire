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

import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.cfg.ApplicationCfg;
import net.openhft.chronicle.wire.cfg.MapInstallable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.openhft.chronicle.core.io.Closeable.closeQuietly;

/**
 * Simple, cut down bootstrap for application configuration.
 * <p>
 * The is a cut down version of Chronicle Engine's EngineMain.
 */
public class Main {
    static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    static <I extends MapInstallable> void addClass(Class<I>... iClasses) {
        ClassAliasPool.CLASS_ALIASES.addAlias(iClasses);
    }

    public static void main(String[] args) throws IOException {
        addClass(ApplicationCfg.class);

        String name = args.length > 0 ? args[0] : "engine.yaml";
        TextWire yaml = TextWire.fromFile(name);
        MapInstallable installable = (MapInstallable) yaml.readObject();
        Map<String, Object> root = Collections.synchronizedMap(new LinkedHashMap<>());
        try {
            installable.install("/", root);
            LOGGER.info("Engine started");

        } catch (Exception e) {
            LOGGER.error("Error starting a component, stopping", e);
            close(root);
        }
    }

    private static void close(Object o) {
        if (o instanceof Closeable) {
            closeQuietly(o);
        } else if (o instanceof Map) {
            for (Object o2 : ((Map) o).values()) {
                close(o2);
            }
        }
    }
}
