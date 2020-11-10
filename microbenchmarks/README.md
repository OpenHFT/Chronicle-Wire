# Wire Format abstraction library - Benchmarks
===

# Purpose

Chronicle Wire supports a separation of describing what data you want to store and retrieve
   and how it should be rendered/parsed.
   Wire handles a variety of formatting options for a wide range of formats.

How do these options affect performance?

The tests were run with -Xmx1g -XX:MaxInlineSize=400 on an isolated CPU on an i7-3970X with 32 GB of memory.

# Tests

These are latency test with the [full Results Here](https://github.com/OpenHFT/Chronicle-Wire/tree/master/microbenchmarks/results)

Something I found interesting is that while a typical time might be stable for a given run, you can get different results at different time.  
For this reason I ran the tests with 10 forks.  By looking at the high percentiles, we tend to pick up the results of the worst run.

| Wire Format | Text encoding | Fixed width values? | Numeric Fields? | field-less?| Bytes | 99.9 %tile | 99.99 %tile | 99.999 %tile | worst |
|--------------|:---------------:|:---------------------:|:----------------:|:-----------:|-----:|-----------:|-------------:|---------------:|---------:|
| YAML (TextWire) | UTF-8     |  false                      | false                | false        | 91    |  2.81       | 4.94           | 8.62            |  17.2     |
| YAML (TextWire) | 8-bit       |  false                      | false                | false        | 91    |  2.59       | 4.70          | 8.58            |  16.8     |
| JSONWire   | 8-bit              |  false                      | false                | false        | 100   |  3.11       | 5.56          | 10.62           |  36.9    |
| BinaryWire   |  UTF-8           | false                      | false                | false        | 70    |  1.57       | 3.42          | 7.14             |  35.1     |
| BinaryWire   |  UTF-8           | false                      | true                 | false        | 44    |  0.67       | 2.44          | 5.93             |  12.1     |
| BinaryWire   |  UTF-8           | true                      | false                 | false        | 84    |  1.51        | 3.32          | 7.22            |  37.4     |
| BinaryWire   |  UTF-8           | true                      | true                  | false        | 57    |  0.57        | 2.26          | 5.09            |  17.5     |
| BinaryWire   |  UTF-8           | false                      | na                   | true         | 32    |  0.65        | 2.42          | 5.53            |   8.6     |
| RawWire      |  UTF-8           | true                      | na                    | true         | 43    |  0.49        | 2.07          | 4.87            |   8.6      |
| RawWire      |  8-bit             | true                      | na                    | true         | 43    |  0.40        | 0.57          | 2.90            |   7.6     |
| BytesMarshallable |  8-bit     | true                       | na                   | true         | 39    |  0.17        | 0.21          | 2.13            |   6.9     |
| BytesMarshallable + stop bit encoding |  8-bit | true | na                 | true         | 28    |  0.21        | 0.25          | 2.40            |   6.4     |

All times are in micro-seconds

"8-bit" encoding is ISO-8859-1 where characters 0 to 255 are mapped to bytes 0 to 255. I this case, (bytes) string.charAt(0)

In all cases, a 4 bytes header was used to determine the length of message.

## The code for all Wire formats
The code to support Wire can be added to the class itself.  These marshallers can also be added to another classes, but in general you would only do this if you can't modify the class you wish to serialize.
```java
@Override
public void readMarshallable(WireIn wire) throws IllegalStateException {
    wire.read(DataFields.price).float64(setPrice)
            .read(DataFields.flag).bool(setFlag)
            .read(DataFields.text).text(text)
            .read(DataFields.side).asEnum(Side.class, setSide)
            .read(DataFields.smallInt).int32(setSmallInt)
            .read(DataFields.longInt).int64(setLongInt);
}

@Override
public void writeMarshallable(WireOut wire) {
    wire.write(DataFields.price).float64(price)
            .write(DataFields.flag).bool(flag)
            .write(DataFields.text).text(text)
            .write(DataFields.side).asEnum(side)
            .write(DataFields.smallInt).int32(smallInt)
            .write(DataFields.longInt).int64(longInt);
}
```

## The code for BytesMarshallable
The code to support BytesMarshallable can eb added to the class marshalled.  While this is faster than using Wire, there is no option to visualise the data without deserializing it, nor does it directly support schema changes.

```yaml
@Override
public void readMarshallable(Bytes<?> bytes) {
    price = bytes.readDouble();
    longInt = bytes.readLong();
    smallInt = bytes.readInt();
    flag = bytes.readBoolean();
//  side = bytes.readEnum(Side.class);
    side = bytes.readBoolean() ? Side.Buy : Side.Sell;
    bytes.read8bit(text);
}

@Override
public void writeMarshallable(Bytes bytes) {
    bytes.writeDouble(price)
            .writeLong(longInt)
            .writeInt(smallInt)
//          .writeEnum(side)
            .writeBoolean(flag)
            .writeBoolean(side == Side.Buy)
            .write8bit(text);
}
```
There was a small advantage in encoding the side as a boolean rather than an Enum as the later writes the name() as a String.

# Comparison with other libraries

| Wire Format | Text encoding |  Fixed width values?  | Numeric Fields? | field-less?| Bytes | 99.9 %tile | 99.99 %tile | 99.999 %tile | worst |
|--------------|:---------------:|:----------------------:|:-----------------:|:---------:|-------:|------------:|-------------:|--------------:|-------:|
| SBE            | 8-bit              |  true                       | true                  | true         | 43     |   0.31       |       0.44     | 4.11            | 9.2     |
| Jackson       | UTF-8            |  false                       | false                 | false       | 100   |   4.95       |        8.33    | 1,400           | 1,500 |
| BSON         | UTF-8              |  true                       | false                 | false       | 96     |  19.8        |   1,430       | 1,400          | 1,600 |
| Snake YAML | UTF-8            |  false                      | false                 | false        | 89     |  80.3        |   4,067       | 16,000        | 24,000 |
| BOON Json | UTF-8              |  false                      | false                 | false        | 100   |  20.7        |       32.5    | 11,000         | 69,000 |
| Externalizable | UTF-8          |  true                       | false                 | false        | 197   |  25.0       |        29.7     | 85,000        | 120,000 |
| Externalizable with Chronicle Bytes | UTF-8 |  true   | false                 | false        | 197   |  24.5       |        29.3     | 85,000        | 118,000 |

All times are in micro-seconds

## Comparison of JSON formats

| Wire Format          | Bytes | 99.9 %tile | 99.99 %tile | 99.999 %tile | worst |
|---------------------:|------:|------------:|------------:|---------------:|--------:|
| JSONWire             |  100*   |  3.11       |        5.56    | 10.62           |  36.9    |
| Jackson                |  100   |   4.95       |       8.3      | 1,400           | 1,500 |
| Jackson + C-Bytes |  100*   |   2.87       |      10.1     | 1,300           | 1,400 |
| Jackson + C-Bytes Reader/Writer| 100*  |  3.06 | 10.3 |  883           | 1,500 |
| BSON                   | 96     |  19.8        |   1,430       | 1,400          | 1,600 |
| BSON + C-Bytes    | 96*     |  7.47        |       15.1    | 1,400          | 11,600 |
| BOON Json           |  100   |  20.7        |       32.5    | 11,000         | 69,000 |

"C-Bytes" means using a recycled Chronicle Bytes buffer.

Tests with "*" on Bytes mean this has been written to/read from direct memory and won't have a copy overhead when working with NIO such as TCP or Files.

## SBE (Simple Binary Encoding)
SBE performs as well are BytesMarshallable.  Even though it was slower in this test, the difference to too small to draw any conclusions. i.e. in a different use case, a different developer might find the difference reversed.

However, I didn't find SBE simple.  Perhaps this is because I didn't use the generation for different languages, but I found it was non-trivial to use and setup.  
For a flat class with just six fields it generated 9 classes, we have three support classes, and I ended up adding methods to the generated class to get it to perform as efficiently as I wanted.
This is likely to be a lack of understand on my part, though I might not be alone in this.

## Snake YAML
Snake YAML is a fully featured YAML 1.1 parser. The library has to do much more work to support all the features of the YAML standard which necessarily takes longer.  
However, it takes a lot longer which means it may not be suitable if you need performance but you don't need a complete YAML parser.

If you need to decode YAML which could come from any sources, Snake YAML is a better choice.

# What does the format look like?
Here some selected examples.  The UTF-8 encoded and 8-bit encoded tests look the same in these cases as there isn't any characters >= 128.

## Text Wire
The main advantage of YAML based wire format is it is easier to implement with, document and debug.

What you want is the ease of a text wire format but the speed of a binary wire format.  
Being able to switch from one to the other can save you a lot of time in development and support, but still give you the speed you want.

This uses 91 bytes, Note: the "--- !!data" is added by the method to dump the data. This information is encoded in the first 4 bytes which contains the size.

```yaml
--- !!data
price: 1234
flag: true
text: Hello World!
side: Sell
smallInt: 123
longInt: 1234567890
```

## JSON Wire
This wire produces a JSON style output.  It has some YAML based extensions for typed data.

Test json8bit used 100 bytes.
```json
--- !!data
"price":1234,"longInt":1234567890,"smallInt":123,"flag":true,"text":"Hello World!","side":"Sell"
```

## Binary Wire (default)
This binary wire has be automatically decoded to text by  Wires.fromSizePrefixedBlobs(Bytes)

This uses 70 bytes. Note: the "--- !!data #binary" is added by the method to dump the data. This information is encoded in the first 4 bytes which contains the size.
```yaml
--- !!data #binary
price: 1234
flag: true
text: Hello World!
side: Sell
smallInt: 123
longInt: 1234567890
```

## Binary Wire with fixed width fields.
Fixed width fields support binding to values later and updating them atomically.  
Note: if you want to bind to specific values, there is support for this which will also ensure the values are aligned.

This format uses 84 bytes
```yaml
--- !!data #binary
price: 1234
flag: true
text: Hello World!
side: Sell
smallInt: 123
longInt: 1234567890
```

## Binary Wire with field numbers
Numbered fields are more efficient to write and read, but are not as friendly to work with.

Test bwireFTF used 44 bytes.
```yaml
--- !!data #binary
3: 1234
4: true
5: Hello World!
6: Sell
1: 123
2: 1234567890
```

## Binary Wire with field numbers and fixed width values

Test bwireTTF used 58 bytes.
```yaml
--- !!data #binary
3: 1234
4: true
5: Hello World!
6: Sell
1: 123
2: 1234567890
```

## Binary Wire with variable width values only. 

Test bwireFTT used 32 bytes.
```yaml
--- !!data #binary
1234
true
Hello World!
Sell
123
1234567890
```

## RawWire format
Raw wire format drops all meta data. It must be fixed width as there is no way to use compact types.

Test rwireUTF and rwire8bit used 43 bytes.
```
00000000 27 00 00 00 00 00 00 00  00 48 93 40 B1 0C 48 65 '······· ·H·@··He
00000010 6C 6C 6F 20 57 6F 72 6C  64 21 04 53 65 6C 6C 7B llo Worl d!·Sell{
00000020 00 00 00 D2 02 96 49 00  00 00 00                ······I· ···
```
## BytesMarshallable
The BytesMarshallable uses fixed width data types.

Test bytesMarshallable used 39 bytes.
```
00000000 23 00 00 00 00 00 00 00  00 48 93 40 D2 02 96 49 #······· ·H·@···I
00000010 00 00 00 00 7B 00 00 00  59 00 0C 48 65 6C 6C 6F ····{··· Y··Hello
00000020 20 57 6F 72 6C 64 21                              World!
```

## BytesMarshallable with stop bit encoding
This example used stop bit encoding to reduce the size of the message.

Test bytesMarshallable used 28 bytes.
```
00000000 18 00 00 00 A0 A4 69 D2  85 D8 CC 04 7B 59 00 0C ······i· ····{Y··
00000010 48 65 6C 6C 6F 20 57 6F  72 6C 64 21             Hello Wo rld!
```

# Comparison outputs

## SnakeYAML
Snake YAML used the .0 on the end of the price to signify that it was a double.  
This added two characters but is an elegant way of encoding that it should be a double.

```yaml
flag: true
longInt: 1234567890
price: 1234.0
side: Sell
smallInt: 123
text: Hello World!
```

## Boon
Test boon used 100 chars.
```json
{"smallInt":123,"longInt":1234567890,"price":1234.0,"flag":true,"side":"Sell","text":"Hello World!"}
```

## Jackson
Test jackson used 100 chars.
```
{"price":1234.0,"flag":true,"text":"Hello World!","side":"Sell","smallInt ":123,"longInt":1234567890}
```
## BSON
Test bson used 96 chars.
```
00000000 60 00 00 00 01 70 72 69  63 65 00 00 00 00 00 00 `····pri ce······
00000010 48 93 40 08 66 6C 61 67  00 01 02 74 65 78 74 00 H·@·flag ···text·
00000020 0D 00 00 00 48 65 6C 6C  6F 20 57 6F 72 6C 64 21 ····Hell o World!
00000030 00 02 73 69 64 65 00 05  00 00 00 53 65 6C 6C 00 ··side·· ···Sell·
00000040 10 73 6D 61 6C 6C 49 6E  74 00 7B 00 00 00 12 6C ·smallIn t·{····l
00000050 6F 6E 67 49 6E 74 00 D2  02 96 49 00 00 00 00 00 ongInt·· ··I·····
```

## SBE
SBE has a method to extract the binary as text. It is likely this data structure could be optimised and made much shorter.

Test sbe used 43 chars.
```
00000000 29 00 7B 00 00 00 D2 02  96 49 00 00 00 00 00 00 )·{····· ·I······
00000010 00 00 00 48 93 40 01 0C  48 65 6C 6C 6F 20 57 6F ···H·@·· Hello Wo
00000020 72 6C 64 21 00 00 00 00  01 00 00                rld!···· ···     
```

## Externalizable
While Externalizable is more efficient than Serializable, it is still a heavy weight serialization.  
Where Java Serialization does well is in serializing Object Graphs instead of Object Tree, i.e. objects with circular references.

Test externalizable used 293 chars.
```
00000000 AC ED 00 05 73 72 00 2A  6E 65 74 2E 6F 70 65 6E ····sr·* net.open
00000010 68 66 74 2E 63 68 72 6F  6E 69 63 6C 65 2E 77 69 hft.chro nicle.wi
00000020 72 65 2E 62 65 6E 63 68  6D 61 72 6B 73 2E 44 61 re.bench marks.Da
00000030 74 61 FB 5E C8 1F BA EB  33 6F 0C 00 00 78 70 77 ta·^···· 3o···xpw
00000040 15 40 93 48 00 00 00 00  00 00 00 00 00 49 96 02 ·@·H···· ·····I··
00000050 D2 00 00 00 7B 01 7E 72  00 2A 6E 65 74 2E 6F 70 ····{·~r ·*net.op
00000060 65 6E 68 66 74 2E 63 68  72 6F 6E 69 63 6C 65 2E enhft.ch ronicle.
00000070 77 69 72 65 2E 62 65 6E  63 68 6D 61 72 6B 73 2E wire.ben chmarks.
00000080 53 69 64 65 00 00 00 00  00 00 00 00 12 00 00 78 Side···· ·······x
00000090 72 00 0E 6A 61 76 61 2E  6C 61 6E 67 2E 45 6E 75 r··java. lang.Enu
000000a0 6D 00 00 00 00 00 00 00  00 12 00 00 78 70 74 00 m······· ····xpt·
000000b0 04 53 65 6C 6C 74 00 0C  48 65 6C 6C 6F 20 57 6F ·Sellt·· Hello Wo
000000c0 72 6C 64 21 78 60 00 00  00 01 70 72 69 63 65 00 rld!x`·· ··price·
000000d0 00 00 00 00 00 48 93 40  08 66 6C 61 67 00 01 02 ·····H·@ ·flag···
000000e0 74 65 78 74 00 0D 00 00  00 48 65 6C 6C 6F 20 57 text···· ·Hello W
000000f0 6F 72 6C 64 21 00 02 73  69 64 65 00 05 00 00 00 orld!··s ide·····
00000100 53 65 6C 6C 00 10 73 6D  61 6C 6C 49 6E 74 00 7B Sell··sm allInt·{
00000110 00 00 00 12 6C 6F 6E 67  49 6E 74 00 D2 02 96 49 ····long Int····I
00000120 00 00 00 00 00                                   ·····                  
```