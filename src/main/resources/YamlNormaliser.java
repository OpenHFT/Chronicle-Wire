import net.openhft.chronicle.bytes.BytesIn;

import java.util.Arrays;

public class YamlNormaliser {
    private final BytesIn in;
    private int indentN = 0;
    private int[] indentLevel = new int[10];
    private boolean newLineIndent = true,
            newLineField = true,
            pushedNewline = false;

    public YamlNormaliser(BytesIn in) {
        this.in = in;
        indentLevel[0] = -1;
    }

    public int next() {
        long pos = in.readPosition();
        if (newLineIndent) {
            // start of line
            int n = readSpaces();
            int i = in.peekUnsignedByte();
            newLineIndent = false;
            if (i == '-') {
                in.readSkip(1);
                int indent = indent();
                if (n > indent) {
                    pushIndent(n, false);
                    return '[';
                }
                if (n < indent) {
                    popIndent();
                    return ']';
                }
                return ',';
            }
            int indent = indent();
            boolean field = field();
            // need more than one.
            if (n < indent || indent == FIELD)
                in.readPosition(pos);
            if (indent == FIELD) {
                popIndent();
                return '}';
            }
            if (n <= indent) {
                popIndent();
                return ']';
            }
        }
        if (pushedNewline) {
            pushedNewline = false;
            return '\n';
        }

        int i = in.readUnsignedByte();
        if (i < 0 && indentN > 0) {
            popIndent();
            return ']';
        }
        if (i == '\n' || i == '\r') {
            newLineIndent = true;
            pushedNewline = true;
            newLineField = true;
            return ' ';
        }
        if (newLineField && i > ' ') {
            newLineField = false;
            boolean field = peekIsField();
            if (field)
                pushIndent(in);
        }
        return i;
    }

    private boolean field() {
        return false;
    }

    public boolean peekIsField() {
        long pos2 = in.readPosition();
        boolean field;
        while (true) {
            int i2 = in.readUnsignedByte();
            if (i2 < ' ' || i2 == ',' || i2 == '!' || i2 == '{' || i2 == '[' || i2 == '}' || i2 == ']') {
                field = false;
                break;
            } else if (i2 == ':' && in.peekUnsignedByte() <= ' ') {
                field = true;
                break;
            }
        }
        in.readPosition(pos2);
        return field;
    }

    private void pushIndent(int i) {
        if (++indentN == indentLevel.length)
            indentLevel = Arrays.copyOf(indentLevel, indentN * 2);
        indentLevel[indentN] = i;
    }

    private void popIndent() {
        assert indentN > 0;
        indentN--;
    }


    private int indent() {
        return indentLevel[indentN];
    }

    private int readSpaces() {
        int i = 0;
        while (true) {
            int ch = in.peekUnsignedByte();
            if (ch > ' ' || ch < 0)
                break;
            in.readSkip(1);
            if (ch == ' ')
                i++;
            else
                i = 0;
        }
        return i;
    }
}
