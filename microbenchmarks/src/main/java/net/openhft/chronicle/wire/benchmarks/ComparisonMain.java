/*
 *     Copyright (C) 2015-2020 chronicle.software
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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.parser.JSONParser;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
public class ComparisonMain {
    final Yaml yaml;
    final ExternalizableData data = new ExternalizableData(123, 1234567890L, 1234, true, "Hello World!", Side.Sell);
    private final ByteBuffer allocate = ByteBuffer.allocate(64);
    private final UnsafeBuffer buffer = new UnsafeBuffer(allocate);
    ExternalizableData data2 = new ExternalizableData();
    String s;
    StringBuilder sb = new StringBuilder();
    JSONParser jsonParser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
    // {"smallInt":123,"longInt":1234567890,"price":1234.0,"flag":true,"text":"Hello World","side":"Sell"}
    com.fasterxml.jackson.core.JsonFactory jsonFactory = new com.fasterxml.jackson.core.JsonFactory(); // or, for data binding, org.codehaus.jackson.mapper.MappingJsonFactory
    UnsafeBuffer directBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(128));
    Bytes bytes = Bytes.allocateDirect(512).unchecked(true);
    InputStream inputStream = bytes.inputStream();
    OutputStream outputStream = bytes.outputStream();
    Writer writer = bytes.writer();
    Reader reader = bytes.reader();
    JsonGenerator generator;
    JsonParser jp;
    JsonParser textJP;
    private byte[] buf;

    public ComparisonMain() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Representer representer = new Representer();
        yaml = new Yaml(new Constructor(Data.class), representer, options);
        try {
            jp = jsonFactory.createParser(inputStream);
            textJP = jsonFactory.createParser(reader);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static void main(String... args) throws Exception {
//        Affinity.setAffinity(2);
        if (false && Jvm.isDebug()) {
            ComparisonMain main = new ComparisonMain();
            for (Method m : ComparisonMain.class.getMethods()) {
                main.s = null;
                main.sb.setLength(0);
                main.buf = null;

                if (m.getAnnotation(Benchmark.class) != null) {
                    m.invoke(main);
                    String s = main.s;
                    if (s != null) {
                        System.out.println("Test " + m.getName() + " used " + s.length() + " chars.");
                        System.out.println(s);
                    } else if (main.sb.length() > 0) {
                        System.out.println("Test " + m.getName() + " used " + main.sb.length() + " chars.");
                        System.out.println(main.sb);
                    } else if (main.buf != null) {
                        System.out.println("Test " + m.getName() + " used " + main.buf.length + " chars.");
                        System.out.println(Bytes.wrapForRead(main.buf).toHexString());
                    } else if (main.bytes.writePosition() > 0) {
                        main.bytes.readPosition(0);
                        System.out.println("Test " + m.getName() + " used " + main.bytes.readRemaining() + " chars.");
                        System.out.println(main.bytes.toHexString());
                    }
                }
            }
        } else {
            int time = Jvm.getBoolean("longTest") ? 30 : 2;
            System.out.println("measurementTime: " + time + " secs");
            Options opt = new OptionsBuilder()
                    .include(ComparisonMain.class.getSimpleName())
                    .warmupIterations(5)
                    .measurementIterations(5)
                    .threads(2)
                    .forks(2)
                    .mode(Mode.SampleTime)
                    .warmupTime(TimeValue.seconds(1))
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
    public ExternalizableData jsonSmart() throws net.minidev.json.parser.ParseException {
        JSONObject obj = new JSONObject();
        data.writeTo(obj);
        s = obj.toJSONString();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(s);
        data2.readFrom(jsonObject);
        return data2;
    }

    // fails on Java 8, https://code.google.com/p/json-smart/issues/detail?id=56&thanks=56&ts=1439401767
    //    @Benchmark
    public void jsonSmartCompact() throws net.minidev.json.parser.ParseException {
        JSONObject obj = new JSONObject();
        data.writeTo(obj);
        s = obj.toJSONString(JSONStyle.MAX_COMPRESS);
        JSONObject jsonObject = (JSONObject) jsonParser.parse(s);
        data.readFrom(jsonObject);
    }

//    @Benchmark
    public ExternalizableData jackson() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator generator = jsonFactory.createGenerator(baos);
        data.writeTo(generator);
        generator.flush();

        buf = baos.toByteArray();
        JsonParser jp = jsonFactory.createParser(buf); // or URL, Stream, Reader, String, byte[]
        data2.readFrom(jp);
        return data2;
    }

//    @Benchmark
    public ExternalizableData jacksonWithCBytes() throws IOException {
        bytes.clear();
        generator = jsonFactory.createGenerator(outputStream);
        data.writeTo(generator);
        generator.flush();

        jp.clearCurrentToken();
        data2.readFrom(jp);
        return data2;
    }

//    @Benchmark
    public ExternalizableData jacksonWithTextCBytes() throws IOException {
        bytes.clear();
        generator = jsonFactory.createGenerator(writer);
        data.writeTo(generator);
        generator.flush();

        textJP.clearCurrentToken();
        data2.readFrom(textJP);
        return data2;
    }

//    @Benchmark
    public Object externalizable() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(data);

        buf = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        }
    }

//    @Benchmark
    public Object externalizableWithCBytes() throws IOException, ClassNotFoundException {
        bytes.clear();
        ObjectOutputStream oos = new ObjectOutputStream(outputStream);
        oos.writeObject(data);

        try (ObjectInputStream ois = new ObjectInputStream(inputStream)) {
            return ois.readObject();
        }
    }
}
