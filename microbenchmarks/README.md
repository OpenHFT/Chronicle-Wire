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
