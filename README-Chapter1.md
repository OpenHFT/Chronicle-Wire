# Wire Format examples

## Simple example

First you need to have a buffer to write to.  This can be a `byte[]`, a `ByteBuffer`, off heap memory, or even an address and length you have obtained from some other library.

```java
// Bytes which wraps a ByteBuffer which is resized as needed.
Bytes<ByteBuffer> textBytes = Bytes.elasticByteBuffer();
```

Now you can choose which format you are using.  As the wire formats are themselves unbuffered, you can use them with the same buffer, but in general using one wire format is easier.

```java
Wire textWire = new TextWire(textBytes);
// or
WireType wireType = WireType.TEXT;
Wire wireB = wireType.apply(textBytes);
// or
Bytes<ByteBuffer> binaryBytes = Bytes.elasticByteBuffer();
Wire binaryWire = new BinaryWire(binaryBytes);
// or
Bytes<ByteBuffer> rawBytes = Bytes.elasticByteBuffer();
Wire rawWire = new RawWire(rawBytes);
```

So now you can write to the wire with a simple document.

```java
textWire.write(() -> "message").text("Hello World")
      .write(() -> "number").int64(1234567890L)
       .write(() -> "code").asEnum(TimeUnit.SECONDS)
      .write(() -> "price").float64(10.50);
System.out.println(textBytes);
```

prints

```yaml
message: Hello World
number: 1234567890
code: SECONDS
price: 10.5
```

Using `toHexString` prints out a binary file hex view of the buffer's contents.

```java
// the same code as for text wire
binaryWire.write(() -> "message").text("Hello World")
        .write(() -> "number").int64(1234567890L)
        .write(() -> "code").asEnum(TimeUnit.SECONDS)
        .write(() -> "price").float64(10.50);
        System.out.println(binaryBytes.toHexString());

// to obtain the underlying ByteBuffer to write to a Channel
ByteBuffer byteBuffer = binaryBytes.underlyingObject();
byteBuffer.position(0);
byteBuffer.limit(binaryBytes.length());
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

```java
// the same code as for text wire
rawWire.write(() -> "message").text("Hello World")
        .write(() -> "number").int64(1234567890L)
        .write(() -> "code").asEnum(TimeUnit.SECONDS)
        .write(() -> "price").float64(10.50);
System.out.println(rawBytes.toHexString());
```

prints in RawWire

```
00000000 0B 48 65 6C 6C 6F 20 57  6F 72 6C 64 D2 02 96 49 ·Hello W orld···I
00000010 00 00 00 00 07 53 45 43  4F 4E 44 53 00 00 00 00 ·····SEC ONDS····
00000020 00 00 25 40                                      ··%@ 
```

## simple example with a data type

See below for the code for Data.  It is much the same and the previous section, with the code required wrapped in a method.

```java
// Bytes which wraps a ByteBuffer which is resized as needed.
Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

Wire wire = new TextWire(bytes);

Data data = new Data("Hello World", 1234567890L, TimeUnit.NANOSECONDS, 10.50);
data.writeMarshallable(wire);
System.out.println(bytes);

Data data2= new Data();
data2.readMarshallable(wire);
System.out.println(data2);
```

prints

```
message: Hello World
number: 1234567890
code: NANOSECONDS
price: 10.5

Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}
```

To write in binary instead

```java
Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
Wire wire2 = new BinaryWire(bytes2);

data.writeMarshallable(wire2);
System.out.println(bytes2.toHexString());

Data data3= new Data();
data3.readMarshallable(wire2);
System.out.println(data3);
```

prints

```
00000000 C7 6D 65 73 73 61 67 65  EB 48 65 6C 6C 6F 20 57 ·message ·Hello W
00000010 6F 72 6C 64 C6 6E 75 6D  62 65 72 A3 D2 02 96 49 orld·num ber····I
00000020 C8 74 69 6D 65 55 6E 69  74 EB 4E 41 4E 4F 53 45 ·timeUni t·NANOSE
00000030 43 4F 4E 44 53 C5 70 72  69 63 65 90 00 00 28 41 CONDS·pr ice···(A

Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}
```

## simple example with a data type

In this example we the data is marshalled as a nested data structure. 

```java
// Bytes which wraps a ByteBuffer which is resized as needed.
Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

Wire wire = new TextWire(bytes);

Data data = new Data("Hello World", 1234567890L, TimeUnit.NANOSECONDS, 10.50);
wire.write(() -> "mydata").marshallable(data);
System.out.println(bytes);

Data data2= new Data();
wire.read(() -> "mydata").marshallable(data2);
System.out.println(data2);

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
Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
Wire wire2 = new BinaryWire(bytes2);

wire2.write(() -> "mydata").marshallable(data);
System.out.println(bytes2.toHexString());

Data data3= new Data();
wire2.read(() -> "mydata").marshallable(data3);
System.out.println(data3);
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

## simple example with a data type with a type

In this example, the type is encoded with the data.  
Instead of showing the entire package name which will almost certainly not work on any other platform, an alias for the type is used.  
It also means the message is shorter and faster.

```java
// Bytes which wraps a ByteBuffer which is resized as needed.
Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

Wire wire = new TextWire(bytes);

ClassAliasPool.CLASS_ALIASES.addAlias(Data.class);

Data data = new Data("Hello World", 1234567890L, TimeUnit.NANOSECONDS, 10.50);
wire.write(() -> "mydata").object(data);
System.out.println(bytes);

Data data2= wire.read(() -> "mydata").object(Data.class);
System.out.println(data2);
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
Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
Wire wire2 = new BinaryWire(bytes2);

wire2.write(() -> "mydata").object(data);
System.out.println(bytes2.toHexString());

Data data3 = wire2.read(() -> "mydata").object(Data.class);
System.out.println(data3);
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

## Write a message with a thread safe size prefix

The benefits of using this approach are that
 - the reader can block until the message is complete.
 - if you have concurrent writers, they will block unless the size if know in which case it skip the message(s) still being written.
 
```java
// Bytes which wraps a ByteBuffer which is resized as needed.
Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

Wire wire = new TextWire(bytes);

ClassAliasPool.CLASS_ALIASES.addAlias(Data.class);

Data data = new Data("Hello World", 1234567890L, TimeUnit.NANOSECONDS, 10.50);
wire.writeDocument(false, data);
System.out.println(Wires.fromSizePrefixedBlobs(bytes));

Data data2= new Data();
assertTrue(wire.readDocument(null, data2));
System.out.println(data2);
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
Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
Wire wire2 = new BinaryWire(bytes2);

wire2.writeDocument(false, data);
System.out.println(Wires.fromSizePrefixedBlobs(bytes2));

Data data3= new Data();
assertTrue(wire2.readDocument(null, data3));
System.out.println(data3);
```

prints

```yaml
--- !!data #binary
message: Hello World
number: 1234567890
timeUnit: NANOSECONDS
price: 10.5

Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}
```

## Write a message with a sequence of records

```java
// Bytes which wraps a ByteBuffer which is resized as needed.
Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

Wire wire = new TextWire(bytes);

ClassAliasPool.CLASS_ALIASES.addAlias(Data.class);

Data[] data = {
        new Data("Hello World", 98765, TimeUnit.HOURS, 1.5),
        new Data("G'Day All", 1212121, TimeUnit.MINUTES, 12.34),
        new Data("Howyall", 1234567890L, TimeUnit.SECONDS, 1000)
};
wire.writeDocument(false, w -> w.write(() -> "mydata")
        .sequence(v -> Stream.of(data).forEach(v::object)));
System.out.println(Wires.fromSizePrefixedBlobs(bytes));

List<Data> dataList = new ArrayList<>();
assertTrue(wire.readDocument(null, w -> w.read(() -> "mydata")
.sequence(v -> { while(v.hasNextSequenceItem()) dataList.add(v.object(Data.class)); })));
dataList.forEach(System.out::println);
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
Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
Wire wire2 = new BinaryWire(bytes2);

wire2.writeDocument(false, w -> w.write(() -> "mydata")
        .sequence(v -> Stream.of(data).forEach(v::object)));
System.out.println(Wires.fromSizePrefixedBlobs(bytes2));

List<Data> dataList2 = new ArrayList<>();
assertTrue(wire2.readDocument(null, w -> w.read(() -> "mydata")
        .sequence(v -> { while(v.hasNextSequenceItem()) dataList2.add(v.object(Data.class)); })));
dataList2.forEach(System.out::println);
```

prints

```yaml
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

The code for the class Data

```java
class Data implements Marshallable {
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

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IllegalStateException {
        wire.read(()->"message").text(s -> message = s)
                .read(() -> "number").int64(i -> number = i)
                .read(() -> "timeUnit").asEnum(TimeUnit.class, e -> timeUnit = e)
                .read(() -> "price").float64(d -> price = d);
    }

    @Override
    public void writeMarshallable(WireOut wire) {
        wire.write(() -> "message").text(message)
                .write(() -> "number").int64(number)
                .write(() -> "timeUnit").asEnum(timeUnit)
                .write(() -> "price").float64(price);
    }

    @Override
    public String toString() {
        return "Data{" +
                "message='" + message + '\'' +
                ", number=" + number +
                ", timeUnit=" + timeUnit +
                ", price=" + price +
                '}';
}
```

