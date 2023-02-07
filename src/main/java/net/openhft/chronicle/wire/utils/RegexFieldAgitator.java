/*
 * Copyright 2016-2022 chronicle.software
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

package net.openhft.chronicle.wire.utils;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

public class RegexFieldAgitator implements YamlAgitator {
    private final String name;
    private final Pattern pattern;
    private final String replaceAll;

    public RegexFieldAgitator(String name, String regex, String replaceAll) {
        this.name = name;
        this.pattern = Pattern.compile(regex);
        this.replaceAll = replaceAll;
    }

    @Override
    public Map<String, String> generateInputs(String yaml) {
        String yaml2 = pattern.matcher(yaml).replaceAll(replaceAll);
        if (yaml2.equals(yaml))
            return Collections.emptyMap();
        return Collections.singletonMap("=\n" +
                        "# " + name + "\n" +
                        yaml2,
                name.replaceAll("[: ]+", "_"));
    }
}
