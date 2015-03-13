package net.openhft.chronicle.wire;

import java.util.Arrays;
import java.util.IntSummaryStatistics;

/**
 * Created by peter.lawrey on 15/01/15.
 */
public enum WireType {
    // Sequence
    BYTES_LENGTH8(Codes.BYTES_LENGTH8),
    BYTES_LENGTH16(Codes.BYTES_LENGTH16),
    BYTES_LENGTH32(Codes.BYTES_LENGTH32),
    BYTES_LENGTH64(Codes.BYTES_LENGTH64), // reserved
    SNAPPY(Codes.SNAPPY),
    DEFLATER(Codes.DEFLATER),
    U8_ARRAY(Codes.U8_ARRAY),
    U16_ARRAY(Codes.U16_ARRAY),
    U32_ARRAY(Codes.U32_ARRAY),
    U64_ARRAY(Codes.U64_ARRAY),
    PADDING32(Codes.PADDING32),
    PADDING(Codes.PADDING),

    // floating point
    FLOAT32(Codes.FLOAT32),
    FLOAT64(Codes.FLOAT64),
    FIXED1(Codes.FIXED1),
    FIXED2(Codes.FIXED2),
    FIXED3(Codes.FIXED3),
    FIXED4(Codes.FIXED4),
    FIXED5(Codes.FIXED5),
    FIXED6(Codes.FIXED6),
    
    // long number
    UUID(Codes.UUID),
    UTF8(Codes.UTF8),
    INT8(Codes.INT8),
    INT16(Codes.INT16),
    INT32(Codes.INT32),
    INT64(Codes.INT64),
    UINT8(Codes.UINT8),
    UINT16(Codes.UINT16),
    UINT32(Codes.UINT32),
    FIXED_6(Codes.FIXED_6),
    FIXED_5(Codes.FIXED_5),
    FIXED_4(Codes.FIXED_4),
    FIXED_3(Codes.FIXED_3),
    FIXED_2(Codes.FIXED_2),
    FIXED_1(Codes.FIXED_1),
    FIXED(Codes.FIXED),

    // pseudo string types.
    COMMENT(Codes.COMMENT),
    HINT(Codes.HINT),
    TIME(Codes.TIME),
    ZONED_DATE_TIME(Codes.ZONED_DATE_TIME),
    DATE(Codes.DATE),
    TYPE(Codes.TYPE),
    FIELD_NAME_ANY(Codes.FIELD_NAME_ANY),
    STRING_ANY(Codes.STRING_ANY),
    FIELD_NUMBER(Codes.FIELD_NUMBER),

    // Boolean
    NULL(Codes.NULL),
    FALSE(Codes.FALSE),
    TRUE(Codes.TRUE),

    // Field string
    FIELD_NAME0(Codes.FIELD_NAME0),
    FIELD_NAME31(Codes.FIELD_NAME31),

    // String type.
    STRING0(Codes.STRING0),
    STRING30(Codes.STRING30),

    END_OF_BYTES(Codes.END_OF_BYTES);

    static final class Codes {
        static final int BYTES_LENGTH8 = 0x80;
        static final int BYTES_LENGTH16 = 0x81;
        static final int BYTES_LENGTH32 = 0x82;
        static final int BYTES_LENGTH64 = 0x83;
        static final int SNAPPY = 0x84;
        static final int DEFLATER = 0x85;
        static final int U8_ARRAY = 0x8A;
        static final int U16_ARRAY = 0x8B;
        static final int U32_ARRAY = 0x8C;
        static final int U64_ARRAY = 0x8D;
        static final int PADDING32 = 0x8E;
        static final int PADDING = 0x8F;

        static final int FLOAT32 = 0x90;
        static final int FLOAT64 = 0x91;
        static final int FIXED1 = 0x92;
        static final int FIXED2 = 0x93;
        static final int FIXED3 = 0x94;
        static final int FIXED4 = 0x95;
        static final int FIXED5 = 0x96;
        static final int FIXED6 = 0x97;
        // 0x9A - 0x9F

        static final int UUID = 0xA0;
        static final int UTF8 = 0xA1;
        static final int INT8 = 0xA2;
        static final int INT16 = 0xA3;
        static final int INT32 = 0xA4;
        static final int INT64 = 0xA5;
        static final int UINT8 = 0xA6;
        static final int UINT16 = 0xA7;
        static final int UINT32 = 0xA8;
        static final int FIXED_6 = 0xA9;
        static final int FIXED_5 = 0xAA;
        static final int FIXED_4 = 0xAB;
        static final int FIXED_3 = 0xAC;
        static final int FIXED_2 = 0xAD;
        static final int FIXED_1 = 0xAE;
        static final int FIXED = 0xAF;

        // 0xB0
        static final int COMMENT = 0xB1;
        static final int HINT = 0xB2;
        static final int TIME = 0xB3;
        static final int ZONED_DATE_TIME = 0xB4;
        static final int DATE = 0xB5;
        static final int TYPE = 0xB6;
        static final int FIELD_NAME_ANY = 0xB7;
        static final int STRING_ANY = 0xB8;
        static final int FIELD_NUMBER = 0xB9;
        // 0xBA, BB, BC
        static final int NULL = 0xBD;
        static final int FALSE = 0xBE;
        static final int TRUE = 0xBF;

        static final int FIELD_NAME0 = 0xC0;
        // ...
        static final int FIELD_NAME31 = 0xDF;

        static final int STRING0 = 0xE0;
        // ...
        static final int STRING30 = 0xFF;

        static final int END_OF_BYTES = -1;

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

        private Codes() {}
    }

    static WireType[] typesByCode;
    static void init() {
        WireType[] values = values();
        IntSummaryStatistics codeStats =
                Arrays.stream(values).mapToInt(WireType::code).summaryStatistics();
        int minCode = codeStats.getMin();
        assert minCode == Codes.END_OF_BYTES;
        int maxCode = codeStats.getMax();
        assert maxCode <= 0xFF;
        assert Arrays.stream(values).distinct().count() == values.length;
        typesByCode = new WireType[maxCode - minCode + 1];
        for (WireType type : values) {
            typesByCode[type.code - minCode] = type;
        }
    }

    public static void main(String[] args) {
        init();
    }


    final int code;

    WireType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static WireType forCode(int code) {
        try {
            return typesByCode[code - Codes.END_OF_BYTES];
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public static String stringForCode(int code) {
        WireType wt = forCode(code);
        return wt == null ? "code:" + Integer.toHexString(code) : wt.name();
    }
}
