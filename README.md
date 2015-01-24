Wire Format abstraction library
===

# Purpose

Chronicle Wire allows the caller to describe the data to be stored and retrieved, 
and the Wire handles the formatting options for a wide range of formats.

# Support
This library will require Java 8, future version may support C++ and C\#

# Text Formats

The text formats include
* JSON
* YAML (a subset of mapping structures included)
* XML
* possible FIX support

# Binary Formats

The binary formats include
* BSON (Binary JSon)
* Binary YAML
* typed data without fields.
* raw data

Options for Binary format
* fixed width with zero copy support.
* variable width

# Compression Options

* no compression
* Snappy compression
* LZW compression

# Bytes options

Wire is built on top of the Bytes library, however Bytes in turn can wrap

* ByteBuffer - heap and direct
* byte\[\] (via ByteBuffer)

# Uses

Wire will be used for

* file headers
* TCP connection headers
* message/excerpt contents.
* the next version of Chronicle Queue
* the API for marshalling generated data types.