package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.BytesIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class YamlTokeniser {

    private static final int INIT_SIZE = 10;
    static final Set<YamlToken> NO_TEXT = EnumSet.of(
            YamlToken.SEQUENCE_START,
            YamlToken.SEQUENCE_ENTRY,
            YamlToken.SEQUENCE_END,
            YamlToken.MAPPING_START,
            YamlToken.MAPPING_KEY,
            YamlToken.MAPPING_END,
            YamlToken.DIRECTIVES_END);
    private final BytesIn in;
    private final List<YamlToken> pushed = new ArrayList<>();
    private int lastContext = 0;
    private YamlToken[] contextArray = new YamlToken[INIT_SIZE];
    private int[] contextIndent = new int[INIT_SIZE];
    @NotNull
    private YamlToken last = YamlToken.NONE;
    private long lineStart = 0;
    private long blockStart = 0; // inclusive
    private long blockEnd = 0; // inclusive
    private int flowDepth = Integer.MAX_VALUE;
    private static final int NO_INDENT = -1;

    public YamlToken context() {
        return contextArray[lastContext];
    }

    public YamlToken current() {
        if (last == YamlToken.NONE)
            next();
        return last;
    }

    @NotNull
    public YamlToken next() {
        if (!pushed.isEmpty()) {
            YamlToken next = popPushed();
            return last = next;
        }
        YamlToken next = next0();
        return this.last = next;
    }

    public YamlTokeniser(BytesIn in) {
        this.in = in;
        contextArray[0] = YamlToken.NONE;
        contextIndent[0] = NO_INDENT;
    }

    private YamlToken next0() {
        consumeWhitespace();
        blockStart = blockEnd = in.readPosition();
        int indent = Math.toIntExact(in.readPosition() - lineStart);
        int ch = in.readUnsignedByte();
        switch (ch) {
            case NO_INDENT:
                if (context() == YamlToken.NONE)
                    return YamlToken.NONE;
                popAll();
                context(YamlToken.NONE);
                return popPushed();
            case '#':
                readComment();
                return YamlToken.COMMENT;
            case '"':
                readQuoted('"');
                if (isFieldEnd())
                    return indent(YamlToken.MAPPING_START, YamlToken.MAPPING_KEY, YamlToken.TEXT, indent * 2);
                return YamlToken.TEXT;
            case '\'':
                readQuoted('\'');
                if (isFieldEnd())
                    return indent(YamlToken.MAPPING_START, YamlToken.MAPPING_KEY, YamlToken.TEXT, indent * 2);

                return YamlToken.TEXT;

            case '?': {
                YamlToken indent2 = indent(YamlToken.MAPPING_START, YamlToken.MAPPING_KEY, YamlToken.NONE, indent * 2);
                contextPush(YamlToken.MAPPING_KEY, indent * 2);
                return indent2;
            }

            case '-': {
                int next = in.peekUnsignedByte();
                if (next <= ' ') {
                    return indent(YamlToken.SEQUENCE_START, YamlToken.SEQUENCE_ENTRY, YamlToken.NONE, indent * 2 + 1);
                }
                if (next == '-') {
                    if (in.peekUnsignedByte(in.readPosition() + 1) == '-' &&
                            in.peekUnsignedByte(in.readPosition() + 2) <= ' ') {
                        in.readSkip(2);
                        pushed.add(YamlToken.DIRECTIVES_END);
                        popAll();
                        contextPush(YamlToken.DIRECTIVES_END, NO_INDENT);
                        return popPushed();
                    }
                }
                in.readSkip(-1);
                return readText(indent);
            }
            case '.': {
                int next = in.peekUnsignedByte();
                if (next == '.') {
                    if (in.peekUnsignedByte(in.readPosition() + 1) == '.' &&
                            in.peekUnsignedByte(in.readPosition() + 2) <= ' ') {
                        in.readSkip(2);
                        popAll();
                        context(YamlToken.NONE);
                        return popPushed();
                    }
                }
                in.readSkip(-1);
                return readText(indent);
            }
            case '&':
                if (in.peekUnsignedByte() > ' ') {
                    readAnchor();
                    return YamlToken.ANCHOR;
                }
                break;
            case '*':
                if (in.peekUnsignedByte() > ' ') {
                    readAnchor();
                    return YamlToken.ALIAS;
                }
            case '|':
                readLiteral();
                return YamlToken.TEXT;
            case '>':
                readFolded();
                return YamlToken.TEXT;
            case '%':
                readDirective();
                return YamlToken.DIRECTIVE;
            case '@':
            case '`':
                readReserved();
                return YamlToken.RESERVED;
            case '!':
                return YamlToken.TAG;
            case '{':
                return flow(YamlToken.MAPPING_START, indent);
            case '}':
                return flowPop(YamlToken.MAPPING_START, '}');
            case '[':
                return flow(YamlToken.SEQUENCE_START, indent);
            case ']':
                return flowPop(YamlToken.SEQUENCE_START, ']');
            case ',':
                // CHECK in a LIST or MAPPING.
                return next0();

            case ':':
                if (in.peekUnsignedByte() <= ' ') {
                    int pos = pushed.size();
                    while (context() != YamlToken.MAPPING_KEY) {
                        contextPop();
                    }
                    contextPop();
                    reversePushed(pos);
                    return pushed.isEmpty() ? next0() : popPushed();
                }
                // other symbols
            case '+':
            case '$':
            case '(':
            case ')':
            case '/':
            case ';':
            case '<':
            case '=':
            case '\\':
            case '^':
            case '_':
            case '~':
        }
        in.readSkip(-1);
        return readText(indent);
    }

    private YamlToken flowPop(YamlToken start, char end) {
        int pos = pushed.size();
        while (context() != start) {
            if (lastContext == 0)
                throw new IllegalArgumentException("Unexpected '" + end + '\'');
            contextPop();
        }
        contextPop();
        reversePushed(pos);
        return popPushed();
    }

    private void readReserved() {
        throw new UnsupportedOperationException();
    }

    private void readDirective() {
        throw new UnsupportedOperationException();
    }

    private void readFolded() {
        throw new UnsupportedOperationException();
    }

    private void readLiteral() {
        throw new UnsupportedOperationException();
    }

    private void readAnchor() {
        blockStart = in.readPosition();
        while (true) {
            blockEnd = in.readPosition();
            int ch = in.readUnsignedByte();
            if (ch <= ' ')
                return;
        }

    }

    private YamlToken flow(YamlToken token, int indent) {
        pushed.add(token);
        if (context() == YamlToken.SEQUENCE_START)
            pushed.add(YamlToken.SEQUENCE_ENTRY);
        contextPush(token, indent);
        if (flowDepth > lastContext)
            flowDepth = lastContext;
        return popPushed();
    }

    private YamlToken readText(int indent) {
        readWords();
        if (isFieldEnd())
            return indent(YamlToken.MAPPING_START, YamlToken.MAPPING_KEY, YamlToken.TEXT, indent * 2);

        if (context() == YamlToken.SEQUENCE_START && isInFlow()) {
            pushed.add(YamlToken.TEXT);
            return YamlToken.SEQUENCE_ENTRY;
        }
        return YamlToken.TEXT;
    }

    private void endOfWords() {
        in.readSkip(-1);
    }

    private YamlToken indent(@NotNull YamlToken indented, @NotNull YamlToken key, @NotNull YamlToken push, int indent) {
        if (push != YamlToken.NONE)
            this.pushed.add(push);
        if (isInFlow()) {
            return key;
        }
        int pos = this.pushed.size();
        while (indent < contextIndent()) {
            contextPop();
        }
        if (indent != contextIndent())
            this.pushed.add(indented);
        this.pushed.add(key);
        reversePushed(pos);
        if (indent > contextIndent())
            contextPush(indented, indent);
        return popPushed();
    }

    private boolean isInFlow() {
        return lastContext >= flowDepth;
    }

    private void reversePushed(int pos) {
        for (int i = pos, j = pushed.size() - 1; i < j; i++, j--)
            pushed.set(i, pushed.set(j, pushed.get(i)));
    }

    private void popAll() {
        int pos = pushed.size();
        while (lastContext > 0) {
            contextPop();
        }
        reversePushed(pos);
    }

    private void readWords() {
        blockStart = in.readPosition();
        while (in.readRemaining() > 0) {
            int ch = in.readUnsignedByte();
            switch (ch) {
                case ':':
                    if (in.peekUnsignedByte() > ' ')
                        continue;
                    // is a field.
                    endOfWords();
                    return;
                case ',':
                    if (context() != YamlToken.SEQUENCE_START
                            && context() != YamlToken.MAPPING_START)
                        continue;
                    endOfWords();
                    return;
                case '[':
                case ']':
                case '{':
                case '}':
                case '#':
                case '\n':
                case '\r':
                    endOfWords();
                    return;
            }
            if (ch > ' ')
                blockEnd = in.readPosition();
        }
    }

    private YamlToken popPushed() {
        return pushed.isEmpty() ? YamlToken.NONE : pushed.remove(pushed.size() - 1);
    }

    private void contextPop() {
        YamlToken context = context();
        lastContext--;
        if (flowDepth == lastContext)
            flowDepth = Integer.MAX_VALUE;
        switch (context) {
            case MAPPING_START:
                pushed.add(YamlToken.MAPPING_END);
                break;
            case SEQUENCE_START:
                pushed.add(YamlToken.SEQUENCE_END);
                break;
            case DIRECTIVES_END:
                pushed.add(YamlToken.DOCUMENT_END);
                break;
            case MAPPING_KEY:
                break;
            case NONE:
                break;
            default:
                throw new IllegalStateException("context: " + context);
        }
    }

    private void contextPush0(YamlToken indented, int indent) {
        lastContext++;
        contextArray[lastContext] = indented;
        contextIndent[lastContext] = indent;
    }

    private int contextIndent() {
        return contextIndent[lastContext];
    }

    private void readQuoted(char stop) {
        blockStart = in.readPosition();
        while (in.readRemaining() > 0) {
            int ch = in.readUnsignedByte();
            if (ch == stop) {
                blockEnd = in.readPosition() - 1;
                return;
            }
            if (ch < ' ') {
                throw new IllegalStateException("Unterminated quotes " + in.subBytes(blockStart - 1, in.readPosition()));
            }
        }
    }

    private boolean isFieldEnd() {
        consumeSpaces();
        if (in.peekUnsignedByte() == ':' &&
                in.peekUnsignedByte(in.readPosition() + 1) <= ' ') {
            in.readSkip(Math.min(2, in.readRemaining()));
            return true;
        }
        return false;
    }

    private void readComment() {
        in.readSkip(1);
        consumeSpaces();
        blockStart = blockEnd = in.readPosition();
        while (true) {
            int ch = in.readUnsignedByte();
            if (ch < 0 || ch == '\n' || ch == '\r')
                return;
            if (ch > ' ')
                blockEnd = in.readPosition();
        }
    }

    private void consumeSpaces() {
        while (true) {
            int ch = in.peekUnsignedByte();
            if (ch == ' ' || ch == '\t') {
                in.readSkip(1);
            } else {
                return;
            }
        }
    }

    private void consumeWhitespace() {
        while (true) {
            int ch = in.peekUnsignedByte();
            if (ch >= 0 && ch <= ' ') {
                in.readSkip(1);
                if (ch == '\n' || ch == '\r')
                    lineStart = in.readPosition();
            } else {
                return;
            }
        }
    }

    public long lineStart() {
        return lineStart;
    }

    public long blockStart() {
        return blockStart;
    }

    public long blockEnd() {
        return blockEnd;
    }

    private void contextPush(YamlToken context, int indent) {
        if (context() == YamlToken.NONE && context != YamlToken.DIRECTIVES_END) {
            contextPush0(YamlToken.DIRECTIVES_END, NO_INDENT);
            contextPush0(context, indent);
            push(YamlToken.DIRECTIVES_END);
            return;
        }
        contextPush0(context, indent);
    }

    // for testing.
    public String text() {
        if (blockStart == blockEnd || NO_TEXT.contains(last))
            return "";
        StringBuilder sb = Wires.acquireStringBuilder();
        long pos = in.readPosition();
        in.readPosition(blockStart);
        in.parseUtf8(sb, Math.toIntExact(blockEnd - blockStart));
        in.readPosition(pos);
        return sb.toString();
    }

    public void push(YamlToken token) {
        pushed.add(token);
    }

    // set the context
    public void context(YamlToken token) {
        contextArray[lastContext] = token;
    }
}
