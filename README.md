Wire Format abstraction library
===

# Purpose

Chronicle Wire supports a separation of describing what data you want to store and retrieve
   and how it should be rendered/parsed.
   Wire handles a variety of formatting options for a wide range of formats.

A key aim of Wire is to support schema changes.  It should make reasonable 
    attempts to handle
* optional fields
* fields in a different order.
* fields the consumer doesn't expect. Optionally parsing them or ignoring them.
* more or less data than expected (in field-less formats) 
* reading a different type to the one written

It should also be as efficient as possible in the case where any or all of these are true
* fields are in the order expected.
* fields are the type expected.
* fields names/numbers are not used.
* self describing types are not needed.
* random access of data values is supported.

# Support
This library will require Java 8. Support for C++ and C\# planned.

# Text Formats

The text formats include
* YAML (a subset of mapping structures included)
* JSON (planned)
* XML (planned)
* FIX (proposed)

Options include
* field names (e.g. JSON) or field numbers (e.g. FIX)
* optional fields with a default values can be dropped.
* zero copy access to fields. (planned)
* thread safe operations in text. (planned)

To support wire format discovery, the first byte should be in the ASCII range,
    adding an ASCII whitespace if needed.
    
# Binary Formats

The binary formats include
* Binary YAML
* typed data without fields.
* raw untyped fieldless data
* BSON (Binary JSon) (planned)

Options for Binary format
* field names or field numbers
* variable width
* optional fields with a default value can be dropped.
* fixed width data with zero copy support.
* thread safe operations.

Note: Wire supports debug/transparent combinations like self describing data with zero copy support.

To support wire format discovery, the first bytes should have the top bit set.

# Compression Options

* no compression
* Snappy compression (planned)
* LZW compression (planned)

# Bytes options

Wire is built on top of the Bytes library, however Bytes in turn can wrap

* ByteBuffer - heap and direct
* byte\[\] (via ByteBuffer)
* raw memory addresses.

# Uses

Wire will be used for

* file headers
* TCP connection headers where the optimal Wire format actually used can be negotiated.
* message/excerpt contents.
* the next version of Chronicle Queue
* the API for marshalling generated data types.

# Similar projects

## SBE

Simple Binary Encoding is designed to do what it says.
    It's simple, it's binary and it supports C++ and Java.  It is 
    designed to be more efficient replacement for FIX. It is not limited to FIX 
    protocols and can be easily extended by updating an XML schema.
    
XML when it first started didn't use XML for it's own schema files, and it not
   insignificant that SBE doesn't use SBE for it's schema either.  This is because it is
   not trying to be human readable, it has XML which though standard isn't designed
   to be particularly human readable either.  Peter Lawrey thinks it's a limitation that it doesn't
   naturally lend itself to a human readable form.
   
The encoding SBE uses is similar to binary with field numbers and fixed width types.  
   SBE assumes the field types which can be more compact than Wire's most similar option 
   (though not as compact as others)
   
SBE has support for schema changes provided the type of a field doesn't change.
   
## msgpack

Message Pack is a packed binary wire format which also supports JSON for 
    human readability and compatibility. It has many similarities to the binary 
    (and JSON) formats of this library.  c.f. Wire is designed to be human readable first, 
    based on YAML, and has a range of options to make it more efficient, 
    the most extreme being fixed position binary.
    
 Msgpack has support for embedded binary, whereas Wire has support for
    comments and hints to improve rendering for human consumption.
    
The documentation looks well thought out, and it is worth emulating.

## Comparison with Cap'n'Proto


| Feature	| Wire Text | Wire Binary | Protobuf	| Cap'n Proto |	SBE	| FlatBuffers |
|------------|:-----------:|:---------------:|:-----------:|:---------------:|:------:|:---------------:|
| Schema evolution |	yes | yes | yes | 	yes	| caveats |	yes |
| Zero-copy | planned | yes | no	| yes	 | yes	 | yes |
|Random-access reads | 	planned | yes | no	 | yes	 | no | 	yes |
|Random-access writes | 	planned | yes | no	 | ?	 | no | 	? |
|Safe against malicious input	| 	yes | yes	| yes		| yes		| yes		| opt-in 	| upfront |
|Reflection / generic algorithms	| 	yes | yes	| yes		| yes		| yes		| yes |
|Initialization order	| any | any	| any	| 	any		| preorder		| bottom-up |
|Unknown field retention	| 	yes |  yes	| yes		| yes		| no		| no |
|Object-capability RPC system	| 	yes | yes	| no		| yes		| no		| no |
|Schema language	| no | no	| custom		| custom		| XML		| custom |
|Usable as mutable state	| 	yes | yes	| yes	| 	no		| no		| no |
|Padding takes space on wire?	| 	yes | yes	| no		| optional	| 	yes		| yes |
|Unset fields take space on wire? | optional | optional		| no		| yes		| yes		| no |
|Pointers take space on wire? | no | no		| no		| yes		| no		| yes |
|C++	| planned | planned	| yes	| 	yes (C++11)*		| yes		| yes |
|Java	 | Java 8 | Java 8	| yes	| 	yes*		| yes		| yes |
|C\#	 | planned | planned	| yes	| 	yes*	| 	yes		| yes* |
|Go | no | no		| yes	| 	yes		| no		| yes* |
|Other languages	lots!	 | no | no | 6+ 	| others*		| no		| no |
|Authors' preferred use case|	distributed  computing | financial / trading	| distributed  computing |	platforms /  sandboxing	| financial / trading	| games |

Based on https://capnproto.org/news/2014-06-17-capnproto-flatbuffers-sbe.html

Note: It not clear what padding which doesn't take up space on the wire means.