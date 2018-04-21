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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

/*
 * Created by Peter Lawrey on 15/08/15.
 */
public class ReadmeChapter1Test {
    @Test
    public void example1() {
        // Bytes which wraps a ByteBuffer which is resized as needed.
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
/*
```

Now you can choice which format you are using.  As the wire formats are themselves unbuffered, you can use them with the same buffer, but in general using one wire format is easier.
```java
 */
        @NotNull Wire wire = new TextWire(bytes);
        // or
        @NotNull WireType wireType = WireType.TEXT;
        Wire wireB = wireType.apply(bytes);
        // or
        Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
        @NotNull Wire wire2 = new BinaryWire(bytes2);
        // or
        Bytes<ByteBuffer> bytes3 = Bytes.elasticByteBuffer();
        @NotNull Wire wire3 = new RawWire(bytes3);
/*
```
So now you can write to the wire with a simple document.
```java
 */
        wire.write(() -> "message").text("Hello World")
                .write(() -> "number").int64(1234567890L)
                .write(() -> "code").asEnum(TimeUnit.SECONDS)
                .write(() -> "price").float64(10.50);
        System.out.println(bytes);
/*
```
prints
```yaml
 */
/*
message: Hello World
number: 1234567890
code: SECONDS
price: 10.5
```

```java
*/
// the same code as for text wire
        wire2.write(() -> "message").text("Hello World")
                .write(() -> "number").int64(1234567890L)
                .write(() -> "code").asEnum(TimeUnit.SECONDS)
                .write(() -> "price").float64(10.50);
        System.out.println(bytes2.toHexString());

// to obtain the underlying ByteBuffer to write to a Channel
        @Nullable ByteBuffer byteBuffer = bytes2.underlyingObject();
        byteBuffer.position(0);
        byteBuffer.limit(bytes2.length());
/*
```

prints
```
00000000 C7 6D 65 73 73 61 67 65  EB 48 65 6C 6C 6F 20 57 ·message ·Hello W
00000010 6F 72 6C 64 C6 6E 75 6D  62 65 72 A3 D2 02 96 49 orld·num ber····I
00000020 C4 63 6F 64 65 E7 53 45  43 4F 4E 44 53 C5 70 72 ·code·SE CONDS·pr
00000030 69 63 65 90 00 00 28 41                          ice···(A
```

Using the RawWire strips away all the meta data to reduce the size of the message, and improve speed.
The down side is that we cannot easily see what the message contains.
*/
        // the same code as for text wire
        wire3.write(() -> "message").text("Hello World")
                .write(() -> "number").int64(1234567890L)
                .write(() -> "code").asEnum(TimeUnit.SECONDS)
                .write(() -> "price").float64(10.50);
        System.out.println(bytes3.toHexString());
/*
```
prints in RawWire
```
00000000 0B 48 65 6C 6C 6F 20 57  6F 72 6C 64 D2 02 96 49 ·Hello W orld···I
00000010 00 00 00 00 07 53 45 43  4F 4E 44 53 00 00 00 00 ·····SEC ONDS····
00000020 00 00 25 40                                      ··%@
```
*/
        bytes.release();
        bytes2.release();
        bytes3.release();
    }

    @Test
    public void example2() {
/*

## simple example with a data type

*/
        // Bytes which wraps a ByteBuffer which is resized as needed.
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

        @NotNull Wire wire = new TextWire(bytes);

        @NotNull Data data = new Data("Hello World", 1234567890L, TimeUnit.NANOSECONDS, 10.50);
        data.writeMarshallable(wire);
        System.out.println(bytes);

        @NotNull Data data2 = new Data();
        data2.readMarshallable(wire);
        System.out.println(data2);

/*
```
prints
```yaml
message: Hello World
number: 1234567890
code: NANOSECONDS
price: 10.5

Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}
```
To write in binary instead
```java
*/
        Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
        @NotNull Wire wire2 = new BinaryWire(bytes2);

        data.writeMarshallable(wire2);
        System.out.println(bytes2.toHexString());

        @NotNull Data data3 = new Data();
        data3.readMarshallable(wire2);
        System.out.println(data3);
/*
```
prints
```
00000000 C7 6D 65 73 73 61 67 65  EB 48 65 6C 6C 6F 20 57 ·message ·Hello W
00000010 6F 72 6C 64 C6 6E 75 6D  62 65 72 A3 D2 02 96 49 orld·num ber····I
00000020 C8 74 69 6D 65 55 6E 69  74 EB 4E 41 4E 4F 53 45 ·timeUni t·NANOSE
00000030 43 4F 4E 44 53 C5 70 72  69 63 65 90 00 00 28 41 CONDS·pr ice···(A

Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}
```
*/
        bytes.release();
        bytes2.release();
    }

    @Test
    public void example3() {
/*
## simple example with a data type
*/
        // Bytes which wraps a ByteBuffer which is resized as needed.
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

        @NotNull Wire wire = new TextWire(bytes);

        @NotNull Data data = new Data("Hello World", 1234567890L, TimeUnit.NANOSECONDS, 10.50);
        wire.write(() -> "mydata").marshallable(data);
        System.out.println(bytes);

        @NotNull Data data2 = new Data();
        wire.read(() -> "mydata").marshallable(data2);
        System.out.println(data2);

/*
```
prints
```yaml
mydata: {
  message: Hello World,
  number: 1234567890,
  timeUnit: NANOSECONDS,
  price: 10.5
}

Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}
```
To write in binary instead
```java
*/
        Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
        @NotNull Wire wire2 = new BinaryWire(bytes2);

        wire2.write(() -> "mydata").marshallable(data);
        System.out.println(bytes2.toHexString());

        @NotNull Data data3 = new Data();
        wire2.read(() -> "mydata").marshallable(data3);
        System.out.println(data3);
/*
```
prints
```
00000000 C6 6D 79 64 61 74 61 82  40 00 00 00 C7 6D 65 73 ·mydata· @····mes
00000010 73 61 67 65 EB 48 65 6C  6C 6F 20 57 6F 72 6C 64 sage·Hel lo World
00000020 C6 6E 75 6D 62 65 72 A3  D2 02 96 49 C8 74 69 6D ·number· ···I·tim
00000030 65 55 6E 69 74 EB 4E 41  4E 4F 53 45 43 4F 4E 44 eUnit·NA NOSECOND
00000040 53 C5 70 72 69 63 65 90  00 00 28 41             S·price· ··(A

Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}
```
*/
        bytes.release();
        bytes2.release();
    }

    @Test
    public void example4() {
/*
## simple example with a data type with a type
```java
*/
        // Bytes which wraps a ByteBuffer which is resized as needed.
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

        @NotNull Wire wire = new TextWire(bytes);

        ClassAliasPool.CLASS_ALIASES.addAlias(Data.class);

        @NotNull Data data = new Data("Hello World", 1234567890L, TimeUnit.NANOSECONDS, 10.50);
        wire.write(() -> "mydata").object(data);
        System.out.println(bytes);

        @Nullable Data data2 = wire.read(() -> "mydata").object(Data.class);
        System.out.println(data2);

/*
```
prints
```yaml
mydata: !Data {
  message: Hello World,
  number: 1234567890,
  timeUnit: NANOSECONDS,
  price: 10.5
}

Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}
```
To write in binary instead
```java
*/
        Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
        @NotNull Wire wire2 = new BinaryWire(bytes2);

        wire2.write(() -> "mydata").object(data);
        System.out.println(bytes2.toHexString());

        @Nullable Data data3 = wire2.read(() -> "mydata").object(Data.class);
        System.out.println(data3);
/*
```
prints
```
00000000 C6 6D 79 64 61 74 61 B6  04 44 61 74 61 82 40 00 ·mydata· ·Data·@·
00000010 00 00 C7 6D 65 73 73 61  67 65 EB 48 65 6C 6C 6F ···messa ge·Hello
00000020 20 57 6F 72 6C 64 C6 6E  75 6D 62 65 72 A3 D2 02  World·n umber···
00000030 96 49 C8 74 69 6D 65 55  6E 69 74 EB 4E 41 4E 4F ·I·timeU nit·NANO
00000040 53 45 43 4F 4E 44 53 C5  70 72 69 63 65 90 00 00 SECONDS· price···
00000050 28 41                                            (A

Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}
```
*/

        bytes.release();
        bytes2.release();
    }

    @Test
    public void example5() {
/*
## Write a message with a thread safe size prefix.

The benefits of using this approach ares that
 - the reader can block until the message is complete.
 - if you have concurrent writers, they will block unless the size if know in which case it skip the message(s) still being written.

```java
*/
        // Bytes which wraps a ByteBuffer which is resized as needed.
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

        @NotNull Wire wire = new TextWire(bytes);

        ClassAliasPool.CLASS_ALIASES.addAlias(Data.class);

        @NotNull Data data = new Data("Hello World", 1234567890L, TimeUnit.NANOSECONDS, 10.50);
        wire.writeDocument(false, data);
        System.out.println(Wires.fromSizePrefixedBlobs(bytes));

        @NotNull Data data2 = new Data();
        assertTrue(wire.readDocument(null, data2));
        System.out.println(data2);

/*
```
prints
```yaml
--- !!data
message: Hello World
number: 1234567890
timeUnit: NANOSECONDS
price: 10.5

Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}
```
To write in binary instead
```java
*/
        Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
        @NotNull Wire wire2 = new BinaryWire(bytes2);
        assert wire2.startUse();

        wire2.writeDocument(false, data);
        System.out.println(Wires.fromSizePrefixedBlobs(bytes2));

        @NotNull Data data3 = new Data();
        assertTrue(wire2.readDocument(null, data3));
        System.out.println(data3);
/*
```
prints
```
--- !!data #binary
message: Hello World
number: 1234567890
timeUnit: NANOSECONDS
price: 10.5

Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}
```
*/
        bytes.release();
        bytes2.release();
    }

    @Test
    public void example6() {
/*
## Write a message with a sequence of records

```java
*/
        // Bytes which wraps a ByteBuffer which is resized as needed.
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

        @NotNull Wire wire = new TextWire(bytes);

        ClassAliasPool.CLASS_ALIASES.addAlias(Data.class);

        @NotNull Data[] data = {
                new Data("Hello World", 98765, TimeUnit.HOURS, 1.5),
                new Data("G'Day All", 1212121, TimeUnit.MINUTES, 12.34),
                new Data("Howyall", 1234567890L, TimeUnit.SECONDS, 1000)
        };
        wire.writeDocument(false, w -> w.write(() -> "mydata")
                .sequence(v -> Stream.of(data).forEach(v::object)));
        System.out.println(Wires.fromSizePrefixedBlobs(bytes));

        @NotNull List<Data> dataList = new ArrayList<>();
        assertTrue(wire.readDocument(null, w -> w.read(() -> "mydata")
                .sequence(dataList, (l, v) -> {
                    while (v.hasNextSequenceItem())
                        l.add(v.object(Data.class));
                })));
        dataList.forEach(System.out::println);

/*
```
prints
```yaml
--- !!data
mydata: [
  !Data {
    message: Hello World,
    number: 98765,
    timeUnit: HOURS,
    price: 1.5
},
  !Data {
    message: G'Day All,
    number: 1212121,
    timeUnit: MINUTES,
    price: 12.34
},
  !Data {
    message: Howyall,
    number: 1234567890,
    timeUnit: SECONDS,
    price: 1000
}
]

Data{message='Hello World', number=98765, timeUnit=HOURS, price=1.5}
Data{message='G'Day All', number=1212121, timeUnit=MINUTES, price=12.34}
Data{message='Howyall', number=1234567890, timeUnit=SECONDS, price=1000.0}
```
To write in binary instead
```java
*/
        Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
        @NotNull Wire wire2 = new BinaryWire(bytes2);
        assert wire2.startUse();
        wire2.writeDocument(false, w -> w.write(() -> "mydata")
                .sequence(v -> Stream.of(data).forEach(v::object)));
        System.out.println(Wires.fromSizePrefixedBlobs(bytes2));

        @NotNull List<Data> dataList2 = new ArrayList<>();
        assertTrue(wire2.readDocument(null, w -> w.read(() -> "mydata")
                .sequence(dataList2, (l, v) -> {
                    while (v.hasNextSequenceItem())
                        l.add(v.object(Data.class));
                })));
        dataList2.forEach(System.out::println);
/*
```
prints
```
--- !!data #binary
mydata: [
  !Data {
    message: Hello World,
    number: 98765,
    timeUnit: HOURS,
    price: 1.5
},
  !Data {
    message: G'Day All,
    number: 1212121,
    timeUnit: MINUTES,
    price: 12.34
},
  !Data {
    message: Howyall,
    number: 1234567890,
    timeUnit: SECONDS,
    price: 1000
}
]

Data{message='Hello World', number=98765, timeUnit=HOURS, price=1.5}
Data{message='G'Day All', number=1212121, timeUnit=MINUTES, price=12.34}
Data{message='Howyall', number=1234567890, timeUnit=SECONDS, price=1000.0}
```
*/
        bytes.release();
        bytes2.release();
    }

    @Test
    public void example7() {
/*

## simple example with a data type

*/
        // Bytes which wraps a ByteBuffer which is resized as needed.
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

        @NotNull Wire wire = new TextWire(bytes);
        ClassAliasPool.CLASS_ALIASES.addAlias(Data.class);

        @NotNull Data data = new Data("Hello World", 1234567890L, TimeUnit.NANOSECONDS, 10.50);
        wire.getValueOut().object(data);
        System.out.println(bytes);

        @Nullable Object o = wire.getValueIn().object(Object.class);
        if (o instanceof Data) {
            @Nullable Data data2 = (Data) o;
            System.out.println(data2);
        }

/*
```
prints
```yaml
!Data {
  message: Hello World,
  number: 1234567890,
  timeUnit: NANOSECONDS,
  price: 10.5
}

Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}
```
To write in binary instead
```java
*/
        Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
        @NotNull Wire wire2 = new BinaryWire(bytes2);

        wire2.getValueOut().object(data);
        System.out.println(bytes2.toHexString());

        @Nullable Object o2 = wire2.getValueIn().object(Object.class);
        if (o2 instanceof Data) {
            @NotNull Data data2 = (Data) o2;
            System.out.println(data2);
        }
/*
```
prints
```
00000000 B6 04 44 61 74 61 82 40  00 00 00 C7 6D 65 73 73 ··Data·@ ····mess
00000010 61 67 65 EB 48 65 6C 6C  6F 20 57 6F 72 6C 64 C6 age·Hell o World·
00000020 6E 75 6D 62 65 72 A3 D2  02 96 49 C8 74 69 6D 65 number·· ··I·time
00000030 55 6E 69 74 EB 4E 41 4E  4F 53 45 43 4F 4E 44 53 Unit·NAN OSECONDS
00000040 C5 70 72 69 63 65 90 00  00 28 41                ·price·· ·(A

Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}
```
*/
        bytes.release();
        bytes2.release();
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }
}

/*
The code for the class Data
```java
*/
class Data extends AbstractMarshallable {
    String message;
    long number;
    TimeUnit timeUnit;
    double price;

    public Data() {
    }

    public Data(String message, long number, TimeUnit timeUnit, double price) {
        this.message = message;
        this.number = number;
        this.timeUnit = timeUnit;
        this.price = price;
    }

/*
```
 */

}