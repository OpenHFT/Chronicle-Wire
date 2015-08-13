# Wire Format abstraction library - Benchmarks
===

# Purpose

Chronicle Wire supports a separation of describing what data you want to store and retrieve
   and how it should be rendered/parsed.
   Wire handles a variety of formatting options for a wide range of formats.

How do these options affect performance?

The tests were run with -Xmx1g for a modest heap size.

# Tests

| Wire Format | Text encoding |  Fixed width values?  | Numeric Fields? | field-less?| Bytes | 99.9 %tile | 99.99 %tile | 99.999 %tile | worst |
|--------------|:---------------:|:----------------------:|:----------------:|:-----------:|-------:|-----------:|-----------:|------------:|---------:|
| YAML (TextWire) | UTF-8     |  false                      | false                 | false        | 90     |  2.81       | 4.94         | 8.62          |  17.2   |
| YAML (TextWire) | 8-bit       |  false                      | false                 | false        | 90      |   2.59     | 4.70         | 8.58        |  16.8     |
| BinaryWire   |  UTF-8           | false                      | false                 | false        | 69      |  1.55      | 3.50         | 7.00         | 14.1      |
| BinaryWire   |  UTF-8           | false                      | na                    | true         | 31      |  0.57      | 2.29         | 5.15         | 10.2      |
| BinaryWire   |  UTF-8           | false                      | true                  | false        | 43    |  0.62       | 2.34         | 5.36         |  7.4       |
| BinaryWire   |  UTF-8           | true                      | false                  | false        | 83     | 1.40       | 3.35         | 6.61         | 20.7      |
| BinaryWire   |  UTF-8           | true                      | true                   | false        | 57      | 0.57       | 2.26         | 5.09         |  17.5     |
| RawWire      |  UTF-8           | true                      | na                     | true        | 42      |  0.40       | 0.60         | 2.94         |   8.6      |
| RawWire      |  8-bit             | true                      | na                     | true        | 42      | 0.40       | 0.52         | 2.65         |   8.2     |
| BytesMarshallable |  8-bit     | true                       | na                    | true        | 42      | 0.18       | 0.23         | 2.18         |   8.3     |
| BytesMarshallable with stop bit encoding |  8-bit  | true | na              | true        | 27      | 0.23       | 0.32         | 2.40         |   9.7     |

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
|--------------|:---------------:|:---------------------:|:----------------:|:-----------:|-------:|-----------:|-------------:|--------------:|-------:|
| SBE            | 8-bit              |  true                       | true                 | true         | 39      |  0.28       | 0.37           | 4.86            | 7.7     |
| Snake YAML | UTF-8            |  false                      | false                 | false        | 88     |  76.1       | 1,493         | 1,522          | 26,673 |
| BOON Json | UTF-16            |  false                      | false                 | false        | 99     |  25.0       | 1,386         | 2,121          | 33,423 |

All times are in micro-seconds

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

This uses 90 bytes, Note: the "--- !!data" is added by the method to dump the data. This information is encoded in the first 4 bytes which contains the size.

```yaml
--- !!data
price: 1234
flag: true
text: Hello World
side: Sell
smallInt: 123
longInt: 1234567890
```

## Binary Wire (default)
This binary wire has be automatically decoded to text by  Wires.fromSizePrefixedBinaryToText(Bytes)

This uses 69 bytes. Note: the "--- !!data #binary" is added by the method to dump the data. This information is encoded in the first 4 bytes which contains the size.
```yaml
--- !!data #binary
price: 1234
flag: true
text: Hello World
side: Sell
smallInt: 123
longInt: 1234567890
```

## Binary Wire with fixed width fields.
Fixed width fields support binding to values later and updating them atomically.  
Note: if you want to bind to specific values, there is support for this which will also ensure the values are aligned.

This format uses 83 bytes
```yaml
--- !!data #binary
price: 1234
flag: true
text: Hello World
side: Sell
smallInt: 123
longInt: 1234567890
```

## Binary Wire with field numbers
Numbered fields are more efficient to write and read, but are not as friendly to work with.

Test bwireFTF used 43 bytes.
```yaml
--- !!data #binary
3: 1234
4: true
5: Hello World
6: Sell
1: 123
2: 1234567890
```

## Binary Wire with field numbers and fixed width values

Test bwireTTF used 57 bytes.
```yaml
--- !!data #binary
3: 1234
4: true
5: Hello World
6: Sell
1: 123
2: 1234567890
```

## Binary Wire with variable width values only. 

Test bwireFTT used 31 bytes.
```yaml
--- !!data #binary
1234
true
Hello World
Sell
123
1234567890
```

## RawWire format
Raw wire format drops all meta data. It must be fixed width as there is no way to use compact types.

Test rwireUTF and rwire8bit used 42 bytes.
```
00000000 26 00 00 00 00 00 00 00  00 48 93 40 B1 0B 48 65 &······· ·H·@··He
00000010 6C 6C 6F 20 57 6F 72 6C  64 04 53 65 6C 6C 7B 00 llo Worl d·Sell{·
00000020 00 00 D2 02 96 49 00 00  00 00                   ·····I·· ··
```
## BytesMarshallable
The BytesMarshallable uses fixed width data types.
```
Test bytesMarshallable used 42 bytes.
00000000 26 00 00 00 00 00 00 00  00 48 93 40 59 0B 48 65 &······· ·H·@Y·He
00000010 6C 6C 6F 20 57 6F 72 6C  64 04 53 65 6C 6C 7B 00 llo Worl d·Sell{·
00000020 00 00 D2 02 96 49 00 00  00 00                   ·····I·· ··
```

## BytesMarshallable with stop bit encoding
This example used stop bit encoding to reduce the size of the message.
```
Test bytesMarshallable used 27 bytes.
00000000 17 00 00 00 A0 A4 69 D2  85 D8 CC 04 7B 59 00 0B ······i· ····{Y··
00000010 48 65 6C 6C 6F 20 57 6F  72 6C 64                Hello Wo rld 
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
text: Hello World
```