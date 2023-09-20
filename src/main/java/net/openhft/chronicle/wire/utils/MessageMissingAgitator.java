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
 * The MessageMissingAgitator class is an implementation of the YamlAgitator interface.
 * This agitator is designed to create altered YAML content by omitting specific messages based on a limit.
 * The purpose is to simulate scenarios where a certain message in the YAML content is missing.
 * <p>
 * A typical use-case might involve testing the resilience of parsers or consumers of the YAML content against
 * missing data.
 *
 * @since 2023-09-16
 */
public class MessageMissingAgitator implements YamlAgitator {

    // Pattern to identify YAML message separators
    static final Pattern SEP = Pattern.compile("[.][.][.]\\s*");

    // Default instance of MessageMissingAgitator with a limit of 4 messages
    static final YamlAgitator INSTANCE = new MessageMissingAgitator(4);

    // The limit on the number of messages that can be made "missing"
    private int limit;

    /**
     * Constructs a new MessageMissingAgitator with the specified limit on missing messages.
     *
     * @param limit The maximum number of messages that can be omitted from the original YAML content.
     */
    public MessageMissingAgitator(int limit) {
        this.limit = limit;
    }

    @Override
    public Map<String, String> generateInputs(String yaml) {
        String[] messages = SEP.split(yaml, 0);
        Map<String, String> ret = new LinkedHashMap<>();
        for (int i = 0; i < messages.length - 1 && i < limit; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("=");
            for (int j = 0; j < messages.length; j++) {
                if (i == j)
                    sb.append("# Missing message ").append(i).append("\n");
                else
                    sb.append(messages[j]).append("...\n");
            }
            ret.put(sb.toString(), "missing-msg-" + i);
        }
        return ret;
    }
}
