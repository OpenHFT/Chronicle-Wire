# Wire Format abstraction library - Benchmarks
===

# Purpose

Chronicle Wire supports a separation of describing what data you want to store and retrieve
   and how it should be rendered/parsed.
   Wire handles a variety of formatting options for a wide range of formats.

How do these options affect performance?

The tests were run with -Xmx1g for a modest heap size.

# Tests

| Wire Format | Text encoding |  Fixed width values?  | Numeric Fields? | field-less?| Bytes | 99.9%tile | 99.99%tile | 99.999%tile |
|--------------|:---------------:|:---------------------:|:----------------:|:-----------:|-------:|---------:|-----------:|------------:|
| YAML (TextWire) | UTF-8     |  false                      | false                 | false        | 90     |  2.81     | 4.94       | 8.62        |
| YAML (TextWire) | 8-bit      |  false                      | false                 | false        | 90      |   2.59    | 4.70       | 8.58        |
| BinaryWire   |  UTF-8           | false                      | false                 | false        | 69      |  1.55    | 3.50       | 7.00         |
| BinaryWire   |  UTF-8           | false                      | na                    | true         | 31      |  0.57    | 2.29       | 5.15         |
| BinaryWire   |  UTF-8           | false                      | true                   | false        | 43    |  0.62     | 2.34       | 5.36         |
| BinaryWire   |  UTF-8           | true                      | false                   | false        | 83     | 1.40     | 3.35       | 6.61         |
| BinaryWire   |  UTF-8           | true                      | true                   | false        | 57      | 0.57     | 2.26       | 5.09         |
| RawWire      |  UTF-8           | true                      | na                     | true        | 42      |  0.40     | 0.60       | 2.94         |
| RawWire      |  8-bit             | true                       | na                     | true        | 42      | 0.40     | 0.52       | 2.65         |

All times are in micro-seconds

"8-bit" encoding is ISO-8859-1 where characters 0 to 255 are mapped to bytes 0 to 255. I this case, (bytes) string.charAt(0)

# The generating code.
In all cases the code which generated this data is the same.  The only thing changes was the Wire format used.

### Writing code
```java
wire.write(DataFields.price).float64(price)
        .write(DataFields.flag).bool(flag)
        .write(DataFields.text).text(text)
        .write(DataFields.side).asEnum(side)
        .write(DataFields.smallInt).int32(smallInt)
        .write(DataFields.longInt).int64(longInt);
```

### Reading code.
```java
wire.read(DataFields.price).float64(setPrice)
        .read(DataFields.flag).bool(setFlag)
        .read(DataFields.text).text(text)
        .read(DataFields.side).asEnum(Side.class, setSide)
        .read(DataFields.smallInt).int32(setSmallInt)
        .read(DataFields.longInt).int64(setLongInt);
```
# What does the format look like?
Here some selected examples.  The UTF-8 encoded and 8-bit encoded tests look the same in these cases as there isn't any characters >= 128.

## Text Wire
This uses 90 bytes

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

This uses 69 bytes.
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
