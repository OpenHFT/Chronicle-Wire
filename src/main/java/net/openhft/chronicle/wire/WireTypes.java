package net.openhft.chronicle.wire;

/**
 * Created by peter on 15/01/15.
 */
public enum WireTypes {
    // control messages
    NULL(0x80),
    DOCUMENT_START(0x81),
    DOCUMENT_END(0x82),
    // Mapping
    MAP_START8(0x83),
    MAP_START16(0x84),
    MAP_START32(0x85),
    // Sequence
    BYTES_LENGTH8(0x86),
    BYTES_LENGTH16(0x87),
    BYTES_LENGTH32(0x88),
    // 0x89 - 0x8C
    PADDING(0x8D),
    COMMENT(0x8E),
    HINT(0x8F),
    // 0x90 - 0x9F

    // data
    TYPE(0xA1),
    UTF8(0xA2),
    INT8(0xA3),
    INT16(0xA4),
    INT32(0xA5),
    INT64(0xA6),
    UINT8(0xA7),
    UINT16(0xA8),
    UINT32(0xA9),

    FLOAT32(0xAA),
    FLOAT64(0xAB),

    TIME(0xAC),
    ZONED_DATE_TIME(0xAD),
    DATE(0xAE),
    // 0xAF

    FIXED_8(0xB0),
    FIXED_7(0xB1),
    FIXED_6(0xB2),
    FIXED_5(0xB3),
    FIXED_4(0xB4),
    FIXED_3(0xB5),
    FIXED_2(0xB6),
    FIXED_1(0xB7),
    FIXED(0xB8),
    FIXED1(0xB9),
    FIXED2(0xBA),
    FIXED3(0xBB),
    FIXED4(0xBC),
    FIXED5(0xBD),
    FIXED6(0xBE),
    FIXED7(0xBF),

    // meta
    FIELD_NUMBER(0xC0),
    FIELD_NAME1(0xC1),
    // ...
    FIELD_NAME30(0xDE),
    FIELD_NAME_ANY(0xDF),

    STRING0(0xE0),
    // ..
    STRING30(0xFE),
    STRING_ANY(0xFF);

    final byte code;

    WireTypes(int code) {
        this.code = (byte) code;
    }
}
