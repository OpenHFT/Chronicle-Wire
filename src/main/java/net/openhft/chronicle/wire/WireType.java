package net.openhft.chronicle.wire;

/**
 * Created by peter on 15/01/15.
 */
public enum WireType {
    // Sequence
    BYTES_LENGTH8(0x80),
    BYTES_LENGTH16(0x81),
    BYTES_LENGTH32(0x82),

    PADDING(0x8D),
    PADDING32(0x8E),
    // EOF of bytes
    DOCUMENT_END(0x8F),

    // floating point
    FLOAT32(0x90),
    FLOAT64(0x91),
    FIXED1(0x92),
    FIXED2(0x93),
    FIXED3(0x94),
    FIXED4(0x95),
    FIXED5(0x96),
    FIXED6(0x97),

    // long number
    UUID(0xA0),
    UTF8(0xA1),
    INT8(0xA2),
    INT16(0xA3),
    INT32(0xA4),
    INT64(0xA5),
    UINT8(0xA6),
    UINT16(0xA7),
    UINT32(0xA8),
    FIXED_6(0xA9),
    FIXED_5(0xAA),
    FIXED_4(0xAB),
    FIXED_3(0xAC),
    FIXED_2(0xAD),
    FIXED_1(0xAE),
    FIXED(0xAF),

    // pseudo string types.
    COMMENT(0xB1),
    HINT(0xB2),
    TIME(0xB3),
    ZONED_DATE_TIME(0xB4),
    DATE(0xB5),
    TYPE(0xB6),
    FIELD_NAME_ANY(0xB7),
    STRING_ANY(0xB8),
    FIELD_NUMBER(0xB9),
    // Boolean
    NULL(0xBD),
    FALSE(0xBE),
    TRUE(0xBF),

    // Field string
    FIELD_NAME0(0xC0),
    // ...
    FIELD_NAME31(0xDF),

    // String type.
    STRING0(0xE0),
    // ...
    STRING30(0xFF),
    UNKNOWN(-1);

    static final int NUM0 = 0x0;
    static final int NUM1 = 0x1;
    static final int NUM2 = 0x2;
    static final int NUM3 = 0x3;
    static final int NUM4 = 0x4;
    static final int NUM5 = 0x5;
    static final int NUM6 = 0x6;
    static final int NUM7 = 0x7;
    static final int CONTROL = 0x8;
    static final int FLOAT = 0x9;
    static final int INT = 0xA;
    static final int SPECIAL = 0xB;
    static final int FIELD0 = 0xC;
    static final int FIELD1 = 0xD;
    static final int STR0 = 0xE;
    static final int STR1 = 0xF;

    final int code;

    WireType(int code) {
        this.code = code;
    }

    public static WireType forCode(int code) {
        for (WireType wireType : values()) {
            if (wireType.code == code)
                return wireType;
        }
        return null;
    }

    public static String stringForCode(int code) {
        WireType wt = forCode(code);
        return wt == null ? "code:" + Integer.toHexString(code) : wt.name();
    }
}
