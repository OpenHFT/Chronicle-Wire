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

import java.util.Map;

public interface YamlAgitator {
    static YamlAgitator messageMissing() {
        return MessageMissingAgitator.INSTANCE;
    }

    static YamlAgitator messageMissing(int limit) {
        return new MessageMissingAgitator(limit);
    }

    static YamlAgitator duplicateMessage() {
        return DuplicateMessageAgitator.INSTANCE;
    }

    static YamlAgitator duplicateMessage(int limit) {
        return new DuplicateMessageAgitator(limit);
    }

    static YamlAgitator missingFields(String... fields) {
        return new MissingFieldAgitator(fields);
    }
    static YamlAgitator overrideFields(String... fields) {
        return new OverrideFieldAgitator(fields);
    }

    static YamlAgitator replaceAll(String name, String regex, String replaceAll) {
        return new RegexFieldAgitator(name, regex, replaceAll);
    }

    Map<String, String> generateInputs(String yaml);
}
