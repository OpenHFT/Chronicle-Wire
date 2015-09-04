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
import net.openhft.affinity.Affinity;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeBytesStore;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.benchmarks.sbe.ExampleUsingGeneratedStub;
import org.boon.json.JsonFactory;
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

/**
 * Created by peter on 12/08/15.
 */
@State(Scope.Thread)
public class ComparisonMain {
    final Yaml yaml;
    final Data data = new Data(123, 1234567890L, 1234, true, "Hello World!", Side.Sell);
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
    Bytes bytes = Bytes.allocateDirect(512).unchecked(true);
    InputStream inputStream = bytes.inputStream();
    OutputStream outputStream = bytes.outputStream();
    Writer writer = bytes.writer();
    Reader reader = bytes.reader();
    JsonGenerator generator;
    JsonParser jp;
    JsonParser textJP;
    JsonParser bsonParser;
    private byte[] buf;

    public ComparisonMain() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Representer representer = new Representer();
        yaml = new Yaml(new Constructor(Data.class), representer, options);
        try {
            jp = jsonFactory.createParser(inputStream);
            textJP = jsonFactory.createParser(reader);
            bsonParser = factory.createJsonParser(inputStream);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static void main(String... args) throws Exception {
        Affinity.setAffinity(2);
        if (Jvm.isDebug()) {
            ComparisonMain main = new ComparisonMain();
            for (Method m : ComparisonMain.class.getMethods()) {
                main.s = null;
                main.sb.setLength(0);
                main.mhe.wrap(main.directBuffer, 0).blockLength(0);
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
                    } else if (main.mhd.wrap(main.directBuffer, 0).blockLength() > 0) {
                        int len = main.mhd.wrap(main.directBuffer, 0).blockLength() + main.mhd.encodedLength();
                        System.out.println("Test " + m.getName() + " used " + len + " chars.");
                        System.out.println(new NativeBytesStore<>(main.directBuffer.addressOffset(), len).bytesForRead().toHexString());
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
            int time = Boolean.getBoolean("longTest") ? 30 : 2;
            System.out.println("measurementTime: " + time + " secs");
            Options opt = new OptionsBuilder()
                    .include(ComparisonMain.class.getSimpleName())
//                    .warmupIterations(5)
                    .measurementIterations(5)
                    .forks(10)
                    .mode(Mode.SampleTime)
                    .measurementTime(TimeValue.seconds(time))
                    .timeUnit(TimeUnit.NANOSECONDS)
                    .build();

            new Runner(opt).run();
        }
    }

        @Benchmark
        public Data snakeYaml() {
            s = yaml.dumpAsMap(data);
            Data data = (Data) yaml.load(s);
            return data;
        }

        // fails on Java 8, https://code.google.com/p/json-smart/issues/detail?id=56&thanks=56&ts=1439401767
    //    @Benchmark
        public Data jsonSmart() throws net.minidev.json.parser.ParseException {
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

        @Benchmark
        public Data boon() {
            sb.setLength(0);
            boonMapper.toJson(data, sb);
            return boonMapper.fromJson(sb.toString(), Data.class);
        }

        @Benchmark
        public Data jackson() throws IOException {
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
        public Data jacksonWithCBytes() throws IOException {
            bytes.clear();
            generator = jsonFactory.createGenerator(outputStream);
            data.writeTo(generator);
            generator.flush();

            jp.clearCurrentToken();
            data2.readFrom(jp);
            return data2;
        }

    @Benchmark
    public Data jacksonWithTextCBytes() throws IOException {
        bytes.clear();
        generator = jsonFactory.createGenerator(writer);
        data.writeTo(generator);
        generator.flush();

        textJP.clearCurrentToken();
        data2.readFrom(textJP);
        return data2;
    }

    @Benchmark
    public Data bson() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator gen = factory.createJsonGenerator(baos);
        data.writeTo(gen);

        buf = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        JsonParser parser = factory.createJsonParser(bais);
        data2.readFrom(parser);
        return data2;
    }

    @Benchmark
    public Data bsonWithCBytes() throws IOException {
        bytes.clear();
        JsonGenerator gen = factory.createJsonGenerator(outputStream);
        data.writeTo(gen);

        bsonParser.clearCurrentToken();
        data2.readFrom(bsonParser);
        return data2;
    }

    @Benchmark
    public Data sbe() {
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
            return data2;
        }
    }

    @Benchmark
    public Data externalizable() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(data);

        buf = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(
                buf);
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (Data) ois.readObject();
        }
    }

    @Benchmark
    public Data externalizableWithCBytes() throws IOException, ClassNotFoundException {
        bytes.clear();
        ObjectOutputStream oos = new ObjectOutputStream(outputStream);
        oos.writeObject(data);

        try (ObjectInputStream ois = new ObjectInputStream(inputStream)) {
            return (Data) ois.readObject();
        }
    }
}
