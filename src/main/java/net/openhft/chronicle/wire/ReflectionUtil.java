/*
 * Copyright 2016-2020 Chronicle Software
 *
 * https://chronicle.software
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
package net.openhft.chronicle.wire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReflectionUtil {
    /**
     * Get all interfaces implemented by all classes (up as far as but not including Object)
     * @param oClass class
     * @return interfaces
     */
    public static List<Class<?>> interfaces(Class<?> oClass) {
        List<Class<?>> list = new ArrayList<>();
        interfaces(oClass, list);
        return list;
    }

    private static void interfaces(Class<?> oClass, List<Class<?>> list) {
        Class<?> baseClass = oClass.getSuperclass();
        if (baseClass == null)
            return;
        list.addAll(Arrays.asList(oClass.getInterfaces()));
        interfaces(baseClass, list);
    }
}
