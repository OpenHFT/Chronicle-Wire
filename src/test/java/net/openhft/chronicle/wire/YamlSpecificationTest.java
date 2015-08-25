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

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * Created by peter on 25/08/15.
 */
@RunWith(Parameterized.class)
public class YamlSpecificationTest {
    private final String input;

    public YamlSpecificationTest(String input) {
        this.input = input;
    }

    @Parameterized.Parameters
    public static Collection tests() {
        return Arrays.asList(new String[][]{
//                {"example2_1"},
//                {"example2_2"},
                {"example2_3"},
//                {"example2_4"} // TODO Fix map format
//                {"example2_5"} // Not supported
//                {"example2_6"} // TODO Fix map format
//                {"example2_7"} // TODO Fix ---
//                {"example2_8"} // TODO Fix ---
//                {"example2_9"} // TODO Fix ---
//                {"example2_10"}// TODO Fix ---
        });
    }

    @Test
    public void decodeAs() throws IOException {
        byte[] byteArr = getBytes(input + ".yaml");
        Bytes bytes = Bytes.wrapForRead(byteArr);
        TextWire tw = new TextWire(bytes);
        Object o = tw.readObject();
        Bytes bytes2 = Bytes.allocateElasticDirect();
        TextWire tw2 = new TextWire(bytes2);
        tw2.writeObject(o);
        byte[] byteArr2 = getBytes(input + ".out.yaml");
        if (byteArr2 == null)
            byteArr2 = byteArr;
        assertEquals(input, Bytes.wrapForRead(byteArr2).toString(), bytes2.toString());
    }

    public byte[] getBytes(String file) throws IOException {
        InputStream is = getClass().getResourceAsStream("/specification/" + file);
        if (is == null) return null;
        int len = is.available();
        byte[] byteArr = new byte[len];
        is.read(byteArr);
        return byteArr;
    }
}
