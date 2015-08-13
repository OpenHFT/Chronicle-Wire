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

package net.openhft.chronicle.wire.benchmarks;

import baseline.DataDecoder;
import baseline.DataEncoder;
import baseline.MessageHeaderDecoder;
import baseline.MessageHeaderEncoder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import de.undercouch.bson4jackson.BsonFactory;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import net.openhft.affinity.Affinity;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.benchmarks.bytes.NativeData;
import org.boon.json.JsonFactory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Created by peter on 12/08/15.
 */
@State(Scope.Thread)
public class ComparisonMain {
    final Yaml yaml;
    final Data data = new Data(123, 1234567890L, 1234, true, "Hello World", Side.Sell);
    private final ByteBuffer allocate = ByteBuffer.allocate(64);
    private final UnsafeBuffer buffer = new UnsafeBuffer(allocate);
    Data data2 = new Data();
    String s;
    StringBuilder sb = new StringBuilder();
    JSONParser jsonParser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
    // {"smallInt":123,"longInt":1234567890,"price":1234.0,"flag":true,"text":"Hello World","side":"Sell"}
    org.boon.json.ObjectMapper boonMapper = JsonFactory.create();
    com.fasterxml.jackson.core.JsonFactory jsonFactory = new com.fasterxml.jackson.core.JsonFactory(); // or, for data binding, org.codehaus.jackson.mapper.MappingJsonFactory
    BsonFactory factory = new BsonFactory();
    UnsafeBuffer directBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(128));
    DataEncoder de = new DataEncoder();
    DataDecoder dd = new DataDecoder();
    MessageHeaderEncoder mhe = new MessageHeaderEncoder();
    MessageHeaderDecoder mhd = new MessageHeaderDecoder();

    public ComparisonMain() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Representer representer = new Representer();
        yaml = new Yaml(new Constructor(Data.class), representer, options);
    }

    public static void main(String... args) throws RunnerException, InvocationTargetException, IllegalAccessException {
        Affinity.setAffinity(2);
        if (Jvm.isDebug()) {
            ComparisonMain main = new ComparisonMain();
            for (Method m : ComparisonMain.class.getMethods()) {
                if (m.getAnnotation(Benchmark.class) != null) {
                    m.invoke(main);
                    String s = main.s;
                    if (s != null) {
                        System.out.println("Test " + m.getName() + " used " + s.length() + " chars.");
                        System.out.println(s);
                    } else {
                        System.out.println("Test " + m.getName() + " used " + main.sb.length() + " chars.");
                        System.out.println(main.sb);

                    }
                }
            }
        } else {
            int time = Boolean.getBoolean("longTest") ? 30 : 2;
            System.out.println("measurementTime: " + time + " secs");
            Options opt = new OptionsBuilder()
                    .include(ComparisonMain.class.getSimpleName())
                    .forks(1)
                    .mode(Mode.SampleTime)
                    .measurementTime(TimeValue.seconds(time))
                    .timeUnit(TimeUnit.NANOSECONDS)
                    .build();

            new Runner(opt).run();
        }
    }

    //    @Benchmark
    public Data snakeYaml() {
        s = yaml.dumpAsMap(data);
        Data data = (Data) yaml.load(s);
        return data;
    }

    // fails on Java 8, https://code.google.com/p/json-smart/issues/detail?id=56&thanks=56&ts=1439401767
//    @Benchmark
    public void jsonSmart() throws ParseException {
        JSONObject obj = new JSONObject();
        data.writeTo(obj);
        s = obj.toJSONString();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(s);
        data.readFrom(jsonObject);

    }

    // fails on Java 8, https://code.google.com/p/json-smart/issues/detail?id=56&thanks=56&ts=1439401767
//    @Benchmark
    public void jsonSmartCompact() throws ParseException {
        JSONObject obj = new JSONObject();
        data.writeTo(obj);
        s = obj.toJSONString(JSONStyle.MAX_COMPRESS);
        JSONObject jsonObject = (JSONObject) jsonParser.parse(s);
        data.readFrom(jsonObject);
    }

    //    @Benchmark
    public Data boon() {
        sb.setLength(0);
        boonMapper.toJson(data, sb);
        return boonMapper.fromJson(sb.toString(), Data.class);
    }

    // need help with this one.
//    @Benchmark
    public Data jackson() throws IOException {
        StringWriter sw = new StringWriter();
        JsonGenerator generator = jsonFactory.createGenerator(sw);
        data.writeTo(generator);
        generator.flush();
        s = sw.toString();
//        System.out.println(s);
        JsonParser jp = jsonFactory.createParser(s); // or URL, Stream, Reader, String, byte[]
        data2.readFrom(jp);
        return data2;
    }

    //    @Benchmark
    public void bson() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator gen = factory.createJsonGenerator(baos);
        data.writeTo(gen);
        s = baos.toString();

        ByteArrayInputStream bais = new ByteArrayInputStream(
                baos.toByteArray());
        JsonParser parser = factory.createJsonParser(bais);
        data2.readFrom(parser);
    }

/*
    @Benchmark
    public void sbe() {
        {
            int len0 = mhe.wrap(directBuffer, 0).encodedLength();
            int len = ExampleUsingGeneratedStub.encode(de, directBuffer, len0, data, allocate, buffer);
            mhe.blockLength(len0 + len);
        }
        {
            mhd.wrap(directBuffer, 0);
            int len0 = mhd.encodedLength();
            int len = mhd.blockLength();
            ExampleUsingGeneratedStub.decode(dd, directBuffer, len0, len, 0, 0, data2);
        }
    }
*/

    //    @Benchmark
    public void byteable() {

        NativeData nd = new NativeData();
        Bytes bytes = Bytes.wrapForWrite(ByteBuffer.allocateDirect(128)).unchecked(true);
        {
            nd.bytesStore(bytes, 4, nd.maxSize());
            data.copyTo(nd);
            bytes.writeInt(nd.encodedLength());
        }
        {
            int len = bytes.readInt();
            nd.bytesStore(bytes, 4, len);
            nd.copyTo(data);
        }
    }
}
