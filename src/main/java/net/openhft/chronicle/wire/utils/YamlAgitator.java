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

/**
 * The YamlAgitator interface defines methods for manipulating and altering YAML content.
 * Implementations of this interface can apply specific changes to YAML input, such as missing fields,
 * duplicating messages, or applying regex transformations.
 * <p>
 * Through its static methods, the interface offers access to various concrete agitator implementations,
 * enabling different ways of agitating YAML content.
 */
@Deprecated(/* to be moved in x.27 */)
public interface YamlAgitator {

    /**
     * Provides a YamlAgitator that handles missing messages.
     *
     * @return An instance of MessageMissingAgitator with default settings.
     */
    static YamlAgitator messageMissing() {
        return MessageMissingAgitator.INSTANCE;
    }

    /**
     * Provides a YamlAgitator that handles missing messages up to a given limit.
     *
     * @param limit The maximum number of missing messages to handle.
     * @return An instance of MessageMissingAgitator set to the provided limit.
     */
    static YamlAgitator messageMissing(int limit) {
        return new MessageMissingAgitator(limit);
    }

    /**
     * Provides a YamlAgitator that duplicates messages.
     *
     * @return An instance of DuplicateMessageAgitator with default settings.
     */
    static YamlAgitator duplicateMessage() {
        return DuplicateMessageAgitator.INSTANCE;
    }

    /**
     * Provides a YamlAgitator that duplicates messages up to a given limit.
     *
     * @param limit The maximum number of messages to duplicate.
     * @return An instance of DuplicateMessageAgitator set to the provided limit.
     */
    static YamlAgitator duplicateMessage(int limit) {
        return new DuplicateMessageAgitator(limit);
    }

    /**
     * Provides a YamlAgitator that flags specified fields as missing.
     *
     * @param fields An array of field names to be flagged as missing.
     * @return An instance of MissingFieldAgitator with the specified missing fields.
     */
    static YamlAgitator missingFields(String... fields) {
        return new MissingFieldAgitator(fields);
    }

    /**
     * Provides a YamlAgitator that overrides the values of specified fields.
     *
     * @param fields An array of field names to be overridden.
     * @return An instance of OverrideFieldAgitator for the specified fields.
     */
    static YamlAgitator overrideFields(String... fields) {
        return new OverrideFieldAgitator(fields);
    }

    /**
     * Provides a YamlAgitator that applies regex transformations on fields.
     *
     * @param name        The name of the field to be transformed.
     * @param regex       The regex pattern to be applied.
     * @param replaceAll  The replacement string.
     * @return An instance of RegexFieldAgitator with the specified transformation parameters.
     */
    static YamlAgitator replaceAll(String name, String regex, String replaceAll) {
        return new RegexFieldAgitator(name, regex, replaceAll);
    }

    /**
     * Generates a map of altered YAML inputs based on the provided YAML string.
     * The key in the returned map represents the modified YAML content, while the value provides a descriptor
     * or label for the specific alteration made.
     *
     * @param yaml The original YAML content to be agitated.
     * @return A map of altered YAML inputs.
     */
    Map<String, String> generateInputs(String yaml);
}
