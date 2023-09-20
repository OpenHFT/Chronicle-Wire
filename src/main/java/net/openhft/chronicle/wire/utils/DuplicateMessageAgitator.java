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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A YAML agitator that generates various permutations of a given YAML content by duplicating specific messages.
 *
 * <p>
 * This agitator uses regular expressions to identify distinct YAML messages within a given input and
 * then duplicates certain messages based on a predefined limit. This can be useful for testing how systems
 * handle duplicated YAML messages or simulating specific scenarios.
 * </p>
 *
 * <p>
 * An example of its usage might be to generate inputs for fuzz testing or to validate how a YAML parser
 * deals with redundant information.
 * </p>
 *
 * @see YamlAgitator
 * @since 2023-09-16
 */
public class DuplicateMessageAgitator implements YamlAgitator {

    /** Pattern to identify the separation between distinct YAML messages. */
    static final Pattern SEP = Pattern.compile("[.][.][.]\\s*");

    /** Singleton instance of this agitator with a default duplication limit. */
    static final YamlAgitator INSTANCE = new DuplicateMessageAgitator(4);

    /** Marker indicating the end of a YAML message. */
    public static final String YAML_EOD = "...\n";

    /** The maximum number of messages to duplicate from the input YAML. */
    private final int limit;

    /**
     * Constructs a new instance with a specified limit on the number of messages to duplicate.
     *
     * @param limit The maximum number of messages to duplicate from the input YAML.
     */
    public DuplicateMessageAgitator(int limit) {
        this.limit = limit;
    }

    @Override
    public Map<String, String> generateInputs(String yaml) {
        String[] messages = SEP.split(yaml, 0);
        Map<String, String> ret = new LinkedHashMap<>();
        for (int i = 0; i < messages.length && i < limit; i++) {
            StringBuilder sb = new StringBuilder();
            String sep = yaml.endsWith(YAML_EOD) ? "" : yaml.endsWith("...") ? "\n" : YAML_EOD;
            sb.append("=")
                    .append(yaml)
                    .append(sep)
                    .append(messages[i])
                    .append(YAML_EOD);
            ret.put(sb.toString(), "msg-" + i + "-duplicated");
        }
        return ret;
    }
}
