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
            LOGGER.warn("Error starting a component, stopping", e);
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
