/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.wire;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by peter on 06/10/15.
 */
public class GenerateFIXWireKey {
    public static void main(String[] args) throws IOException {
        Files.lines(Paths.get("src/test/resources/FIX42.xml"))
                .filter(s -> s.contains("field number='"))
                .map(s -> s.split("'"))
                .forEach(arr -> System.out.printf("\t%s(%s),%n", arr[3], arr[1]));
    }
}
