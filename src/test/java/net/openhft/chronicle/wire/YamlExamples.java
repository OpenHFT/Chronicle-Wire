/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.lang.reflect.Type;
import java.time.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;
import static org.junit.Assert.assertEquals;

/*
 * Created by peter.lawrey on 12/01/15.
 */
public class YamlExamples {
    public static void sequenceExample(@NotNull Wire wire) {
        /*
         - Mark McGwire
         - Sammy Sosa
         - Ken Griffey
         */
        wire.write(Keys.list).sequence(vo -> {
            for (String s : "Mark McGwire,Sammy Sosa,Ken Griffey".split(","))
                vo.text(s);
        });

        // or
/*
        ValueOut valueOut = wire.write(Keys.list).sequenceStart();
        for (String s : "Mark McGwire,Sammy Sosa,Ken Griffey".split(",")) {
            valueOut.text(s);
        }
        valueOut.sequenceEnd();
*/
        // to read this.
        @NotNull List<String> names = new ArrayList<>();
        wire.read(Keys.list).sequence(names, (l, valueIn) -> {
            while (valueIn.hasNext()) {
                l.add(valueIn.text());
            }
        });
    }

    public static void mapExample(@NotNull Wire wire) {
        /*
        american:
          - Boston Red Sox
          - Detroit Tigers
          - New York Yankees
        national:
          - New York Mets
          - Chicago Cubs
          - Atlanta Braves
         */
/*
        wire.sequence(Keys.american, "Boston Red Sox", "Detroit Tigers", "New York Yankees");
        wire.sequence(Keys.national, "New York Mets", "Chicago Cubs", "Atlanta Braves");

        wire.readSequenceStart(Keys.american);
        while (wire.hasNextSequenceItem())
            wire.readText();
        wire.readSequenceEnd();

        List<String> team = new ArrayList<String>();
        wire.readSequence(Keys.national, team, String.class);
*/
        /*
        -
          name: Mark McGwire
          hr:   65
          avg:  0.278
        -
          name: Sammy Sosa
          hr:   63
          avg:  0.288
         */
/*
        wire.writeSequenceStart();
        wire.writeMappingStart();
        wire.writeText(Keys.name, "Mark McGwire");
        wire.writeInt(Keys.hr, 65);
        wire.writeDouble(Keys.avg, 0.278);
        wire.writeMappingEnd();

        wire.writeMappingStart();
        wire.writeText(Keys.name, "Sammy Sosa");
        wire.writeInt(Keys.hr, 63);
        wire.writeDouble(Keys.avg, 0.288);
        wire.writeMappingEnd();
        wire.writeSequenceEnd();

        wire.flip();

        wire.readSequenceStart();
        while (wire.hasNextSequenceItem()) {
            wire.readMapStart();
            String name = wire.readText(Keys.name);
            int hr2 = wire.readInt(Keys.hr);
            double avg2 = wire.readDouble(Keys.avg);
            wire.readMapEnd();
        }
        wire.readSequenceEnd();

*/
        wire.clear();
        /*
        Mark McGwire: {hr: 65, avg: 0.278}
        Sammy Sosa: {
            hr: 63,
            avg: 0.288
          }
         */
/*
        wire.write("Mark McGwire", Keys.name).mapStart()
                .write(Keys.hr).int32(65)
                .write(Keys.avg).float64(0.278)
                .writeMappingEnd();

        wire.write("Sammy Sosa", Keys.name).mapStart()
                .write(Keys.hr).int32(63)
                .write(Keys.avg).float64(0.288)
                .writeMappingEnd();

        wire.flip();

        StringBuilder name = new StringBuilder();
        while (wire.hasMapping()) {
            wire.read(name, Keys.name).mapStart()
                    .read(Keys.hr).int32(stats::hr)
                    .read(Keys.avg).float64(stats::avg)
                    .readMapEnd();
        }
*/
        wire.clear();

        /*
        ---
        time: 20:03:20
        player: Sammy Sosa
        action: strike (miss)
        ...
        ---
        time: 20:03:47
        player: Sammy Sosa
        action: grand slam
        ...
        */
/*
        wire.writeDocumentStart();
        wire.writeTime(Keys.time, LocalTime.of(20, 3, 20));
        wire.writeText(Keys.player, "Sammy Sosa");
        wire.writeText(Keys.action, "strike (miss)");
        wire.writeDocumentEnd();
        wire.writeDocumentStart();
        wire.writeTime(Keys.time, LocalTime.of(20, 3, 47));
        wire.writeText(Keys.player, "Sammy Sosa");
        wire.writeText(Keys.action, "grand slam");
        wire.writeDocumentEnd();

        wire.flip();
        while (wire.hasDocument()) {
            wire.readDocumentStart();
            LocalTime time = wire.readTime(Keys.time);
            String player = wire.readText(Keys.player);
            String action = wire.readText(Keys.action);
            wire.consumeDocumentEnd();
        }
*/
        wire.clear();

        /*
        canonical: 2001-12-15T02:59:43.1Z
        iso8601: 2001-12-14t21:59:43.10-05:00
        spaced: 2001-12-14 21:59:43.10 -5
        date: 2002-12-14
        */

/*
        wire.writeZonedDateTime(Keys.canonical, ZonedDateTime.parseOne("2001-12-15T02:59:43.1Z"));
        ZonedDateTime zdt = wire.readZonedDateTime(Keys.canonical);

        wire.writeDate(Keys.date, LocalDate.of(2002, 12, 14));
        LocalDate ld = wire.readDate();
*/
    }

    public static void object(Wire wire) {
        /*
        !myType {
            name: Hello World
            date: 2015-01-12
         }
         */
/*
        MyType myType = new MyType();
        wire.writeMarshallable(myType);
        wire.flip();
        wire.readMarshallable(myType);
*/
    }

    @Test
    public void testMappedObject() {
        @NotNull Wire wire = new BinaryWire(nativeBytes());
/*
        name: Mark McGwire
        hr:   65    # Home runs
        avg:  0.278 # Batting average
        rbi:  147   # Runs Batted In
*/
        wire.write(Keys.name).text("Mark McGwire")
                .write(Keys.hr).int32(65)
                .writeComment("Home runs")
                .write(Keys.avg).float64(0.278)
                .writeComment("Batting average")
                .write(Keys.rbi).int64(147)
                .writeComment("Runs Batted In");

        @NotNull Stats stats = new Stats();
        wire.read(Keys.name).textTo(stats.name);
        wire.read(Keys.hr)
                .int32(stats, (o, x) -> o.hr = x)
                .read(Keys.avg)
                .float64(stats, (o, x) -> o.avg = x)
                .read(Keys.rbi).int64(stats, (o, x) -> o.rbi = x);
        wire.clear();

        assertEquals("Stats{name=Mark McGwire, hr=65, avg=0.278, rbi=147}", stats.toString());
    }

    enum Keys implements WireKey {
        list(List.class, Collections.emptyList()),
        american(List.class, Collections.emptyList()),
        national(List.class, Collections.emptyList()),
        name(""),
        time(LocalTime.MIN),
        player(""),
        action(""),
        hr(0),
        avg(0.0),
        rbi(0L),
        canonical(ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault())),
        date(LocalDate.MIN);

        static {
            WireKey.checkKeys(values());
        }

        private final Type type;
        private final Object defaultValue;

        Keys(@NotNull Object defaultValue) {
            this(defaultValue.getClass(), defaultValue);
        }

        Keys(Type type, Object defaultValue) {
            this.type = type;
            this.defaultValue = defaultValue;
        }

        @Override
        public Type type() {
            return type;
        }

        @Override
        public Object defaultValue() {
            return defaultValue;
        }
    }

    static class Stats {
        @NotNull
        StringBuilder name = new StringBuilder();
        int hr;
        double avg;
        long rbi;

        @NotNull
        public StringBuilder name() {
            return name;
        }

        public int hr() {
            return hr;
        }

        public void hr(int hr) {
            this.hr = hr;
        }

        public void avg(double avg) {
            this.avg = avg;
        }

        public void rbi(long rbi) {
            this.rbi = rbi;
        }

        @NotNull
        @Override
        public String toString() {
            return "Stats{" +
                    "name=" + name +
                    ", hr=" + hr +
                    ", avg=" + avg +
                    ", rbi=" + rbi +
                    '}';
        }
    }

}

