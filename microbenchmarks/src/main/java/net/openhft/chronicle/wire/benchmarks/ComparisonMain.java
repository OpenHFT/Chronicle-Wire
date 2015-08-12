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

import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import net.openhft.affinity.Affinity;
import net.openhft.chronicle.core.Jvm;
import org.boon.json.JsonFactory;
import org.boon.json.JsonParser;
import org.boon.json.ObjectMapper;
import org.boon.json.implementation.JsonFastParser;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Created by peter on 12/08/15.
 */
@State(Scope.Thread)
public class ComparisonMain {
    final Yaml yaml;
    final Data data = new Data(123, 1234567890L, 1234, true, "Hello World", Side.Sell);
    String s;
    StringBuilder sb = new StringBuilder();
    JSONParser jsonParser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
    // {"smallInt":123,"longInt":1234567890,"price":1234.0,"flag":true,"text":"Hello World","side":"Sell"}
    ObjectMapper mapper = JsonFactory.create();
    JsonParser parser = new JsonFastParser();

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

    @Benchmark
    public Data boon() {
        sb.setLength(0);
        mapper.toJson(data, sb);
        return mapper.fromJson(sb.toString(), Data.class);
    }

}
