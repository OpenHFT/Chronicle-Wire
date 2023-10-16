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

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.parser.JSONParser;
import net.openhft.affinity.AffinityLock;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.*;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.profile.AsyncProfiler;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/*
Benchmark                                          Mode      Cnt          Score      Error  Units
ComparisonMain.externalizable                    sample  5268384      30491.196 ± 1536.478  ns/op
ComparisonMain.externalizable:p0.00              sample                4000.000             ns/op
ComparisonMain.externalizable:p0.50              sample                4160.000             ns/op
ComparisonMain.externalizable:p0.90              sample                4344.000             ns/op
ComparisonMain.externalizable:p0.95              sample                4616.000             ns/op
ComparisonMain.externalizable:p0.99              sample                6400.000             ns/op
ComparisonMain.externalizable:p0.999             sample              128512.000             ns/op
ComparisonMain.externalizable:p0.9999            sample            57409536.000             ns/op
ComparisonMain.externalizable:p1.00              sample           162529280.000             ns/op
ComparisonMain.externalizableWithCBytes          sample  5535996      28117.343 ± 1400.970  ns/op
ComparisonMain.externalizableWithCBytes:p0.00    sample                3840.000             ns/op
ComparisonMain.externalizableWithCBytes:p0.50    sample                4040.000             ns/op
ComparisonMain.externalizableWithCBytes:p0.90    sample                4144.000             ns/op
ComparisonMain.externalizableWithCBytes:p0.95    sample                4248.000             ns/op
ComparisonMain.externalizableWithCBytes:p0.99    sample                4728.000             ns/op
ComparisonMain.externalizableWithCBytes:p0.999   sample                9536.000             ns/op
ComparisonMain.externalizableWithCBytes:p0.9999  sample            54263808.000             ns/op
ComparisonMain.externalizableWithCBytes:p1.00    sample           127139840.000             ns/op
ComparisonMain.fastjson                          sample  5177930       9047.950 ±  881.619  ns/op
ComparisonMain.fastjson:p0.00                    sample                 450.000             ns/op
ComparisonMain.fastjson:p0.50                    sample                 530.000             ns/op
ComparisonMain.fastjson:p0.90                    sample                 540.000             ns/op
ComparisonMain.fastjson:p0.95                    sample                 550.000             ns/op
ComparisonMain.fastjson:p0.99                    sample                 580.000             ns/op
ComparisonMain.fastjson:p0.999                   sample                 980.000             ns/op
ComparisonMain.fastjson:p0.9999                  sample            34316006.195             ns/op
ComparisonMain.fastjson:p1.00                    sample           139460608.000             ns/op
ComparisonMain.jackson                           sample  6151310      11725.651 ±  928.023  ns/op
ComparisonMain.jackson:p0.00                     sample                 850.000             ns/op
ComparisonMain.jackson:p0.50                     sample                 930.000             ns/op
ComparisonMain.jackson:p0.90                     sample                 960.000             ns/op
ComparisonMain.jackson:p0.95                     sample                 970.000             ns/op
ComparisonMain.jackson:p0.99                     sample                1220.000             ns/op
ComparisonMain.jackson:p0.999                    sample                1770.000             ns/op
ComparisonMain.jackson:p0.9999                   sample            42270720.000             ns/op
ComparisonMain.jackson:p1.00                     sample           130547712.000             ns/op
ComparisonMain.jacksonWithCBytes                 sample  9066351       6024.018 ±  524.735  ns/op
ComparisonMain.jacksonWithCBytes:p0.00           sample                 570.000             ns/op
ComparisonMain.jacksonWithCBytes:p0.50           sample                 620.000             ns/op
ComparisonMain.jacksonWithCBytes:p0.90           sample                 640.000             ns/op
ComparisonMain.jacksonWithCBytes:p0.95           sample                 650.000             ns/op
ComparisonMain.jacksonWithCBytes:p0.99           sample                 680.000             ns/op
ComparisonMain.jacksonWithCBytes:p0.999          sample                1090.000             ns/op
ComparisonMain.jacksonWithCBytes:p0.9999         sample            23986176.000             ns/op
ComparisonMain.jacksonWithCBytes:p1.00           sample           111935488.000             ns/op
ComparisonMain.jacksonWithTextCBytes             sample  6277993       8939.602 ±  757.818  ns/op
ComparisonMain.jacksonWithTextCBytes:p0.00       sample                 870.000             ns/op
ComparisonMain.jacksonWithTextCBytes:p0.50       sample                 930.000             ns/op
ComparisonMain.jacksonWithTextCBytes:p0.90       sample                 950.000             ns/op
ComparisonMain.jacksonWithTextCBytes:p0.95       sample                 960.000             ns/op
ComparisonMain.jacksonWithTextCBytes:p0.99       sample                1000.000             ns/op
ComparisonMain.jacksonWithTextCBytes:p0.999      sample                1400.000             ns/op
ComparisonMain.jacksonWithTextCBytes:p0.9999     sample            35979264.000             ns/op
ComparisonMain.jacksonWithTextCBytes:p1.00       sample           100139008.000             ns/op
ComparisonMain.jsonWire                          sample  6268504      11366.721 ±  703.311  ns/op
ComparisonMain.jsonWire:p0.00                    sample                1930.000             ns/op
ComparisonMain.jsonWire:p0.50                    sample                1980.000             ns/op
ComparisonMain.jsonWire:p0.90                    sample                2000.000             ns/op
ComparisonMain.jsonWire:p0.95                    sample                2010.000             ns/op
ComparisonMain.jsonWire:p0.99                    sample                2030.000             ns/op
ComparisonMain.jsonWire:p0.999                   sample                2048.000             ns/op
ComparisonMain.jsonWire:p0.9999                  sample            31981568.000             ns/op
ComparisonMain.jsonWire:p1.00                    sample            71958528.000             ns/op
ComparisonMain.snakeYaml                         sample  1037780     240739.622 ± 9705.738  ns/op
ComparisonMain.snakeYaml:p0.00                   sample               37760.000             ns/op
ComparisonMain.snakeYaml:p0.50                   sample               39936.000             ns/op
ComparisonMain.snakeYaml:p0.90                   sample               47296.000             ns/op
ComparisonMain.snakeYaml:p0.95                   sample               56448.000             ns/op
ComparisonMain.snakeYaml:p0.99                   sample               70784.000             ns/op
ComparisonMain.snakeYaml:p0.999                  sample            53149696.000             ns/op
ComparisonMain.snakeYaml:p0.9999                 sample            84468537.754             ns/op
ComparisonMain.snakeYaml:p1.00                   sample           154664960.000             ns/op

Benchmark                                           Mode      Cnt         Score     Error   Units
ComparisonMain.jsonWireDirect                     sample  6459950      6143.633 ± 518.761   ns/op
ComparisonMain.jsonWireDirect:gc.alloc.rate       sample        5         0.122 ±   0.017  MB/sec
ComparisonMain.jsonWireDirect:gc.alloc.rate.norm  sample        5         0.124 ±   0.018    B/op
ComparisonMain.jsonWireDirect:gc.count            sample        5           ≈ 0            counts
ComparisonMain.jsonWireDirect:p0.00               sample                930.000             ns/op
ComparisonMain.jsonWireDirect:p0.50               sample                970.000             ns/op
ComparisonMain.jsonWireDirect:p0.90               sample                980.000             ns/op
ComparisonMain.jsonWireDirect:p0.95               sample                980.000             ns/op
ComparisonMain.jsonWireDirect:p0.99               sample                990.000             ns/op
ComparisonMain.jsonWireDirect:p0.999              sample               1000.000             ns/op
ComparisonMain.jsonWireDirect:p0.9999             sample           28409856.000             ns/op
ComparisonMain.jsonWireDirect:p1.00               sample           67895296.000             ns/op
 */
/**
 * Compare JSON writing/parsing
 */
@State(Scope.Thread)
public class ComparisonMain {
    final Yaml yaml;
    final ExternalizableData data = new ExternalizableData(123, 1234567890L, 1234.5, true, "Hello World!", Side.Sell);
    ExternalizableData data2 = new ExternalizableData();
    String s;
    StringBuilder sb = new StringBuilder();
    JSONParser jsonParser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
    // {"smallInt":123,"longInt":1234567890,"price":1234.0,"flag":true,"text":"Hello World","side":"Sell"}
    com.fasterxml.jackson.core.JsonFactory jsonFactory = new com.fasterxml.jackson.core.JsonFactory(); // or, for data binding, org.codehaus.jackson.mapper.MappingJsonFactory
    Bytes<?> bytes = Bytes.allocateDirect(512).unchecked(true);
    JSONWire jsonWire = new JSONWire(bytes, true);
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
        Representer representer = new Representer(options);
        LoaderOptions loaderOptions = new LoaderOptions();
        yaml = new Yaml(new Constructor(Data.class, loaderOptions), representer, options);
        try {
            jp = jsonFactory.createParser(inputStream);
            textJP = jsonFactory.createParser(reader);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        bytes.fpAppend0(false);
        jsonWire.useTextDocuments();
    }

    public static void main(String... args) throws Exception {
        if (Jvm.isDebug()) {
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
            try (AffinityLock affinityLock = AffinityLock.acquireLock(Jvm.getProperty("affinity", "any"))) {
                int time = Jvm.getBoolean("longTest") ? 60 : 10;
                System.out.println("measurementTime: " + time + " secs");
                Options opt = new OptionsBuilder()
                        .include(ComparisonMain.class.getSimpleName())
                        .warmupIterations(5)
                        .measurementIterations(5)
                        .threads(5)
                        .forks(1) // use only one fork with affinity
                        .mode(Mode.SampleTime)
                        .warmupTime(TimeValue.seconds(1))
                        .measurementTime(TimeValue.seconds(time))
                        .timeUnit(TimeUnit.NANOSECONDS)
                        .jvmArgsAppend("-Xmx64m")
//                    .addProfiler(GCProfiler.class)
                        .build();

                new Runner(opt).run();
            }
        }
    }

//    @Benchmark
    public Data snakeYaml() {
        s = yaml.dumpAsMap(data);
        Data data = yaml.load(s);
        return data;
    }

    // fails with net.minidev.json.parser.ParseException: Malicious payload, having non natural depths, parsing stoped on { at position 0.
//    @Benchmark
    public ExternalizableData jsonSmart() throws net.minidev.json.parser.ParseException {
        JSONObject obj = new JSONObject();
        data.writeTo(obj);
        s = obj.toJSONString();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(s);
        data2.readFrom(jsonObject);
        return data2;
    }

    // Used to fail on Java 8, https://code.google.com/p/json-smart/issues/detail?id=56&thanks=56&ts=1439401767
    // Now fails with net.minidev.json.parser.ParseException: Unexpected token smallInt at position 9
    //@Benchmark
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

    @Benchmark
    public Object jsonWireDirect() {
        jsonWire.reset();
        ((WriteMarshallable) data).writeMarshallable(jsonWire);
        ((ReadMarshallable) data2).readMarshallable(jsonWire);
        return data;
    }

//    @Benchmark
    public Object jsonWire() {
        jsonWire.reset();
        jsonWire.getValueOut().marshallable((WriteMarshallable) data);
        // below is faster than jsonWire.getValueIn().marshallable((ReadMarshallable) data2) as it does not read length first
        SerializationStrategies.MARSHALLABLE.readUsing(ExternalizableData.class, data2, jsonWire.getValueIn(), BracketType.MAP);
        return data2;
    }

//    @Benchmark
    public Object fastjson() {
        bytes.clear();
        // TODO: JSONWriter
        byte[] ba = JSON.toJSONBytes(data);
        bytes.write(ba);
        return JSON.parseObject(ba, ExternalizableData.class);
    }
}
