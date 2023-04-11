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

public class OverrideFieldAgitator implements YamlAgitator {
    private String[] fields;

    public OverrideFieldAgitator(String... fields) {
        this.fields = fields;
    }

    @Override
    public Map<String, String> generateInputs(String yaml) {
        Map<String, String> ret = new LinkedHashMap<>();
        for (String field : fields) {
            String[] fieldValue = field.split(": ", 2);
            if (fieldValue.length != 2)
                throw new IllegalArgumentException("invalid field: value as " + field + "'");
            if (!yaml.contains(" " + fieldValue[0] + ": "))
                continue;
            String regex = "( +)(" + fieldValue[0] + ": [^,\\n]*)";
            String replacement = "$1# override $2 to " + field.replace("\n", " ") + " \n" +
                    "$1" + field + ",\n" +
                    "$1-$2";
            String yaml2 = yaml.replaceAll(regex, replacement);
            if (yaml2.equals(yaml))
                continue;
            String field2 = field.replaceFirst(": +", "=")
                    .replaceAll("[: \"\']", "_");
            ret.put("=" + yaml2, "set-field-" + field2);
        }
        return ret;
    }
}
