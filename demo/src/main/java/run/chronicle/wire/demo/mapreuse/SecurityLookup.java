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

package run.chronicle.wire.demo.mapreuse;

import java.util.ArrayList;
import java.util.List;

/**
 * This example shows how a List can be sorted and used for looking up keys
 * so that no Maps need to be created. The standard Map implementations create
 * a lot of extra objects when values are put into the Map.
 * <p>
 * The sorted List can be reused over and over again.
 */
public class SecurityLookup {

    public static void main(String[] args) {

        // These can be reused
        final Security s0 = new Security(100, 45, 2);
        final Security s1 = new Security(10, 100, 42);
        final Security s2 = new Security(20, 200, 13);

        // This can be reused
        final List<Security> securities = new ArrayList<>();

        securities.add(s0);
        securities.add(s1);
        securities.add(s2);

        // Reusable Mapper
        IntMapper<Security> mapper =
                new IntMapper<>(Security::getId);
        mapper.set(securities);

        Security security100 = mapper.get(100);

        System.out.println("security100 = " + security100);
    }
}
