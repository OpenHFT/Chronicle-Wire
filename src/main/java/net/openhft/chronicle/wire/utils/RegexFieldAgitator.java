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

/**
 * The RegexFieldAgitator class is an implementation of the YamlAgitator interface.
 * This class allows for a regular expression-based transformation of input YAML content.
 *
 * The agitator applies the specified regex pattern to identify and replace matching segments
 * within the YAML content. This class is useful for scenarios where you need to modify
 * or sanitize certain patterns within YAML strings, especially in testing and validation contexts.
 *
 * @since 2023-09-16
 */
public class RegexFieldAgitator implements YamlAgitator {

    // Name of the agitator, used primarily for identification
    private final String name;

    // Compiled regular expression pattern
    private final Pattern pattern;

    // Replacement string
    private final String replaceAll;

    /**
     * Constructs a new RegexFieldAgitator with the given name, regular expression,
     * and the replacement string.
     *
     * @param name        The name of the agitator.
     * @param regex       The regex pattern to be applied.
     * @param replaceAll  The replacement string for matches found.
     */
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
