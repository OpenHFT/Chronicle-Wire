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
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.parser.JSONParser;
import net.openhft.affinity.Affinity;
import net.openhft.affinity.AffinityLock;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.BracketType;
import net.openhft.chronicle.wire.JSONWire;
import net.openhft.chronicle.wire.SerializationStrategies;
import net.openhft.chronicle.wire.WriteMarshallable;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
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

/* on a Ryzen 5950X, with 8 threads
Benchmark                          Mode  Cnt         Score         Error  Units
ComparisonMain.fastjson           thrpt   33  10986645.275 ± 1033839.257  ops/s
ComparisonMain.jacksonWithCBytes  thrpt   33  11644821.957 ±  186579.126  ops/s
ComparisonMain.jsonBytes          thrpt   33  12630238.718 ±  178770.580  ops/s
ComparisonMain.jsonWire           thrpt   33   3750653.689 ±  256018.536  ops/s
 */
/**
 * Compare JSON writing/parsing
 */
@State(Scope.Thread)
public class ComparisonMain {
    final Yaml yaml;
    final ExternalizableData data = new ExternalizableData(1234.5678, true, "Hello World!", Side.Sell, 123, 1234567890L);
    //private final ByteBuffer allocate = ByteBuffer.allocate(64);
    //private final UnsafeBuffer buffer = new UnsafeBuffer(allocate);
    ExternalizableData data2 = new ExternalizableData();
    String s;
    StringBuilder sb = new StringBuilder();
    JSONParser jsonParser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
    // {"smallInt":123,"longInt":1234567890,"price":1234.5678,"flag":true,"text":"Hello World","side":"Sell"}
    com.fasterxml.jackson.core.JsonFactory jsonFactory = new com.fasterxml.jackson.core.JsonFactory(); // or, for data binding, org.codehaus.jackson.mapper.MappingJsonFactory
    UnsafeBuffer directBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(128));
    Bytes<?> bytes = Bytes.allocateDirect(512).unchecked(true);
    JSONWire jsonWire = new JSONWire(bytes);
    InputStream inputStream = bytes.inputStream();
    OutputStream outputStream = bytes.outputStream();
    Writer writer = bytes.writer();
    Reader reader = bytes.reader();
    JsonGenerator generator;
    JsonParser jp;
    JsonParser textJP;
    private byte[] buf;
    private AffinityLock affinityLock;

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
            int time = Jvm.getBoolean("longTest") ? 30 : 2;
            System.out.println("measurementTime: " + time + " secs");
            String[] jvmArgs = {
                    "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
                    "--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED",
                    "--add-exports=java.base/jdk.internal.util=ALL-UNNAMED",
                    "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED",
                    "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                    "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                    "--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    "--add-opens=jdk.compiler/com.sun.tools.javac=ALL-UNNAMED"};
            if (!Jvm.isJava9Plus())
                jvmArgs = new String[0];

            Options opt = new OptionsBuilder()
                    .include(ComparisonMain.class.getSimpleName())
                    .jvmArgsAppend(jvmArgs)
                    .warmupIterations(3)
                    .measurementIterations(3)
                    .threads(8)
                    .forks(11)
                    .mode(Mode.Throughput)
                    .warmupTime(TimeValue.seconds(1))
                    .measurementTime(TimeValue.seconds(time))
                    .timeUnit(TimeUnit.SECONDS)
                    .build();

            new Runner(opt).run();
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

    @Benchmark
    public ExternalizableData jacksonWithCBytes() throws IOException {
        if (affinityLock == null)
            affinityLock = Affinity.acquireLock();
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
    public Object jsonWire() {
        if (affinityLock == null)
            affinityLock = Affinity.acquireLock();

        jsonWire.reset();
        jsonWire.getValueOut().marshallable((WriteMarshallable) data);
        // below is faster than jsonWire.getValueIn().marshallable((ReadMarshallable) data2) as it does not read length first
        SerializationStrategies.MARSHALLABLE.readUsing(ExternalizableData.class, data2, jsonWire.getValueIn(), BracketType.MAP);
        return data2;
    }

    @Benchmark
    public Object jsonBytes() {
        if (affinityLock == null)
            affinityLock = Affinity.acquireLock();
        bytes.clear();
        data.writeMarshallable(bytes);
        data2.readMarshallable(bytes);
        return data2;
    }

    JSONWriter.Context context = JSON.createWriteContext(SerializeConfig.global, JSON.DEFAULT_GENERATE_FEATURE);

    @Benchmark
    public Object fastjson() {
        if (affinityLock == null)
            affinityLock = Affinity.acquireLock();

        try (JSONWriter writer1 = JSONWriter.ofUTF8(context)) {
            writer1.setRootObject(data);
            Class<?> valueClass = ((Object) data).getClass();
            ObjectWriter objectWriter = context.getObjectWriter(valueClass, valueClass);
            objectWriter.write(writer1, data, null, null, 0);

            byte[] result = writer1.getBytes();

            return JSON.parseObject(result, ExternalizableData.class);
        } catch (com.alibaba.fastjson2.JSONException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new JSONException("toJSONBytes error", cause);
        } catch (RuntimeException ex) {
            throw new JSONException("toJSONBytes error", ex);
        }
    }

    static class Profile {
        static volatile Object bh;

        public static void main(String[] args) {
            ComparisonMain comparisonMain = new ComparisonMain();
            long start = System.currentTimeMillis();
            do {
                bh = comparisonMain.jsonBytes();
            } while (System.currentTimeMillis() < start + 30_000);
        }
    }
}
