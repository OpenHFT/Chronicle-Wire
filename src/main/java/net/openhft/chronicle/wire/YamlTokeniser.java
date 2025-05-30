/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.core.scoped.ScopedResourcePool;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * A tokenizer for YAML documents. The YamlTokeniser class is responsible for
 * converting a raw YAML input into individual tokens, each representing
 * a distinct construct or symbol in YAML. This class is integral to
 * processes such as parsing or tokenization of YAML documents.
 */
@SuppressWarnings({"this-escape","deprecation"})
public class YamlTokeniser {

    /** Represents an undefined or invalid indentation. */
    static final int NO_INDENT = -1;

    /** Set of YAML tokens that don't contain any associated text content. */
    static final Set<YamlToken> NO_TEXT = EnumSet.of(
            YamlToken.SEQUENCE_START,
            YamlToken.SEQUENCE_ENTRY,
            YamlToken.SEQUENCE_END,
            YamlToken.MAPPING_START,
            YamlToken.MAPPING_KEY,
            YamlToken.MAPPING_END,
            YamlToken.DIRECTIVES_END);

    /** A pool of StringBuilders to improve efficiency and reduce memory overhead. */
    static final ScopedResourcePool<StringBuilder> SBP = StringBuilderPool.createThreadLocal(1);

    /** Stack to manage contextual information during tokenization. */
    protected final List<YTContext> contexts = new ArrayList<>();

    /** The input source containing the raw YAML content. */
    private final BytesIn<?> in;

    /** A pool of reusable context objects to manage YAML structures. */
    private final List<YTContext> freeContexts = new ArrayList<>();

    /** List of tokens that have been identified but not yet processed. */
    private final List<YamlToken> pushed = new ArrayList<>();

    /** Temporary bytes buffer. */
    Bytes<?> temp = null;

    /** Position marker for the start of a line. */
    long lineStart;

    /** Position marker for the start of a block. */
    long blockStart;

    /** Position marker for the end of a block. */
    long blockEnd;

    /** Current depth of flow structures, like lists or maps. */
    int flowDepth = Integer.MAX_VALUE;

    /** Character used to denote quoting in a block. */
    char blockQuote = 0;

    /** Flag to indicate if a sequence entry has been encountered. */
    boolean hasSequenceEntry;

    /** Position marker for the last key in a key-value pair. */
    long lastKeyPosition = -1;

    /** The last token that was processed. */
    private YamlToken last = YamlToken.STREAM_START;

    /**
     * Constructs a new YAML tokenizer with the specified input.
     *
     * @param in The input source containing raw YAML content.
     */
    public YamlTokeniser(BytesIn<?> in) {
        reset();
        this.in = in;
    }

    /**
     * Retrieves the number of context objects currently being managed.
     *
     * @return The size of the context list.
     */
    public int contextSize() {
        return contexts.size();
    }

    /**
     * Resets the state of the tokenizer. This method prepares the tokenizer
     * for processing a new input or to restart the tokenization of the current input.
     */
    void reset() {
        contexts.clear();
        freeContexts.clear();
        if (temp != null)
            temp.clear();
        long pos = in == null ? 0 : in.readPosition();
        lineStart = blockStart = blockEnd = pos;
        flowDepth = Integer.MAX_VALUE;
        blockQuote = 0;
        hasSequenceEntry = false;
        lastKeyPosition = -1;
        pushed.clear();
        last = YamlToken.STREAM_START;
        pushContext0(YamlToken.STREAM_START, NO_INDENT);
    }

    /**
     * Returns the context of the YAML tokenization process.
     * This method provides the top-level token context based on the tokenization history.
     *
     * @return The top context token if contexts are present, otherwise returns STREAM_START.
     */
    public YamlToken context() {
        return contexts.isEmpty() ? YamlToken.STREAM_START : topContext().token;
    }

    /**
     * Retrieves the top context from the context stack.
     * This method provides the most recent tokenization context.
     *
     * @return The top YTContext object from the context stack.
     */
    public YTContext topContext() {
        return contexts.get(contextSize() - 1);
    }

    /**
     * Retrieves the second to top context from the context stack.
     * This method provides the tokenization context that's just below the topmost one.
     *
     * @return The second top YTContext object from the context stack.
     */
    public YTContext secondTopContext() {
        return contexts.get(contextSize() - 2);
    }

    /**
     * Gets the current token in the tokenization process.
     * If the last token was the start of the stream, this method fetches the next token.
     *
     * @return The current YamlToken object representing the tokenization status.
     */
    public YamlToken current() {
        if (last == YamlToken.STREAM_START)
            return next(NO_INDENT);
        return last;
    }

    /**
     * Fetches the next token based on the current context's indentation.
     *
     * @return The next YamlToken object in line based on the current indentation context.
     */
    public YamlToken next() {
        return next(contextIndent());
    }

    /**
     * Retrieves the next YAML token considering the minimum indentation provided.
     * This method drives the core tokenization process, fetching tokens based on
     * the minimum indentation and updating the last token processed.
     *
     * @param minIndent The minimum indentation to consider while tokenizing.
     * @return The next YamlToken object based on the specified indentation.
     */
    @NotNull
    public YamlToken next(int minIndent) {
        if (!pushed.isEmpty()) {
            YamlToken next = popPushed();  // Fetching the next token from the pushed list
            return last = next;
        }
        YamlToken next = next0(minIndent); // Internal method to get the next token based on indentation
        return this.last = next;
    }

    /**
     * Core method to tokenize the YAML content based on the given minimum indentation.
     * The method processes the current position in the input stream and returns the
     * next tokenized YAML construct. It utilizes the current context and the indentation
     * level to identify and process different YAML constructs.
     *
     * @param minIndent The minimum indentation level to consider while tokenizing.
     * @return The next {@link YamlToken} in the tokenization sequence.
     */
    YamlToken next0(int minIndent) {
        // Consuming any whitespace present at the start of a line
        consumeWhitespace();

        // Setting the block start and end markers to the current reading position
        blockStart = blockEnd = in.readPosition();

        // Clearing the temporary buffer if present
        if (temp != null)
            temp.clear();

        // Fetching the top context for reference during tokenization
        YTContext context = topContext();

        // Calculating the indentation level for the current block
        int indent2 = Math.toIntExact(in.readPosition() - lineStart) * 2;

        // Reading the next character from the input stream for processing
        int ch = in.readUnsignedByte();

        // Processing the read character to identify the associated YAML token
        switch (ch) {
            case -1:
                if (contextIndent() <= minIndent)
                    return YamlToken.NONE;
                contextPop();
                return popPushed();
            case '#':
                readComment();
                return YamlToken.COMMENT;
            case '"':
                if (wouldChangeContext(minIndent, indent2))
                    return dontRead();
                lastKeyPosition = in.readPosition() - 1;
                readDoublyQuoted();
                if (isFieldEnd())
                    return indent(YamlToken.MAPPING_START, YamlToken.MAPPING_KEY, YamlToken.TEXT, indent2);
                return YamlToken.TEXT;
            case '\'':
                if (wouldChangeContext(minIndent, indent2))
                    return dontRead();
                lastKeyPosition = in.readPosition() - 1;
                readSinglyQuoted();
                if (isFieldEnd())
                    return indent(YamlToken.MAPPING_START, YamlToken.MAPPING_KEY, YamlToken.TEXT, indent2);

                return YamlToken.TEXT;

            case '?': {
                if (wouldChangeContext(minIndent, indent2))
                    return dontRead();
                lastKeyPosition = in.readPosition() - 1;
                YamlToken indentB = indent(YamlToken.MAPPING_START, YamlToken.MAPPING_KEY, YamlToken.STREAM_START, indent2);
                contextPush(YamlToken.MAPPING_KEY, indent2);
                return indentB;
            }

            case '-': {
                int next = in.peekUnsignedByte();
                if (next <= ' ') {
                    if (wouldChangeContext(minIndent, indent2 + 1))
                        return dontRead();

                    hasSequenceEntry = true;
                    return indent(YamlToken.SEQUENCE_START, YamlToken.SEQUENCE_ENTRY, YamlToken.STREAM_START, indent2 + 1);
                }
                if (indent2 == 0 && next == '-' && in.peekUnsignedByte(in.readPosition() + 1) == '-' && in.peekUnsignedByte(in.readPosition() + 2) <= ' ') {
                    if (contextIndent() <= minIndent && minIndent >= 0)
                        return dontRead();
                    in.readSkip(2);
                    pushed.add(YamlToken.DIRECTIVES_END);
                    popAll(1);
                    contextPush(YamlToken.DIRECTIVES_END, NO_INDENT);
                    return popPushed();
                }
                unreadLast();
                return readText(indent2);
            }
            case '.': {
                int next = in.peekUnsignedByte();
                if (indent2 == 0 && next == '.') {
                    if (in.peekUnsignedByte(in.readPosition() + 1) == '.' &&
                            in.peekUnsignedByte(in.readPosition() + 2) <= ' ') {
                        if (contextIndent() <= minIndent)
                            return dontRead();
                        in.readSkip(2);
                        popAll(1);
                        return popPushed();
                    }
                }
                unreadLast();
                return readText(indent2);
            }
            case '&':
                if (in.peekUnsignedByte() > ' ') {
                    readWord();
                    return YamlToken.ANCHOR;
                }
                break;
            case '*':
                if (in.peekUnsignedByte() > ' ') {
                    readWord();
                    return YamlToken.ALIAS;
                }
                break;
            case '|':
                if (in.peekUnsignedByte() <= ' ') {
                    readLiteral();
                    return seq(YamlToken.LITERAL);
                }
                break;
            case '>':
                if (in.peekUnsignedByte() <= ' ') {
                    readFolded();
                    return seq(YamlToken.LITERAL);
                }
                break;
            case '%':
                readDirective();
                return YamlToken.DIRECTIVE;
            case '@':
            case '`':
                readReserved();
                return seq(YamlToken.RESERVED);
            case '!':
                readWord();
                push(seq(YamlToken.TAG));
                if (context() == YamlToken.STREAM_START) {
                    pushContext0(YamlToken.DIRECTIVES_END, NO_INDENT);
                    push(YamlToken.DIRECTIVES_END);
                }
                return popPushed();
            case '{':
                return flow(YamlToken.MAPPING_START);
            case '}':
                if (minIndent == Integer.MAX_VALUE || context.keysCount() > 0) {
                    return dontRead();
                }
                return flowPop(YamlToken.MAPPING_START, '}');
            case '[':
                hasSequenceEntry = false;
                return flow(YamlToken.SEQUENCE_START);
            case ']':
                if (minIndent == Integer.MAX_VALUE)
                    return dontRead();
                return flowPop(YamlToken.SEQUENCE_START, ']');
            case ',':
                if (flowDepth >= contextSize())
                    flowDepth = contextSize();
                hasSequenceEntry = false;
                // CHECK in a LIST or MAPPING.
                return next0(minIndent);

            case ':':
                if (in.peekUnsignedByte() <= ' ') {
                    int pos = pushed.size();
                    while (context() != YamlToken.MAPPING_KEY && contextSize() > 1) {
                        contextPop();
                    }
                    if (context() == YamlToken.MAPPING_KEY)
                        contextPop();
                    reversePushed(pos);
                    return pushed.isEmpty() ? next0(minIndent) : popPushed();
                }
                break;
        // Other symbols that might have specific semantics in certain YAML constructs
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

        // If changing context, don't read the symbol
        if (wouldChangeContext(minIndent, indent2))
            return dontRead();

        // Revert to reading the last character in the input stream
        unreadLast();

        // Tokenize the special symbol as regular text
        return readText(indent2);
    }

    /**
     * Determine if the current context would change given the indentation levels.
     *
     * @param minIndent The minimum indentation to consider.
     * @param indent The current indentation level.
     * @return True if context would change, false otherwise.
     */
    private boolean wouldChangeContext(int minIndent, int indent) {
        if (isInFlow())
            return false;
        return minIndent > indent;
    }

    /**
     * Helper method to handle scenarios where the character shouldn't be tokenized.
     * This method ensures the last read character is reverted back and a NONE token is returned.
     *
     * @return The {@link YamlToken#NONE} token.
     */
    private YamlToken dontRead() {
        unreadLast();
        return YamlToken.NONE;
    }

    /**
     * Pop from the context stack until a specified start token is encountered.
     * This method is useful for flow constructs where we need to determine the
     * boundaries (like a list or map).
     *
     * @param start The token to identify the start of the flow construct.
     * @param end The character representing the end of the flow construct.
     * @return The appropriate {@link YamlToken} after popping the context.
     */
    private YamlToken flowPop(YamlToken start, char end) {
        int pos = pushed.size();
        while (context() != start) {
            if (contextSize() <= 1)
                throw new IllegalArgumentException("Unexpected '" + end + '\'');
            contextPop();
        }
        contextPop();
        reversePushed(pos);
        return popPushed();
    }

    /**
     * Handles YAML flow constructs such as sequences and maps.
     * This method manages the context and the stack of tokens accordingly.
     *
     * @param token The {@link YamlToken} to be processed.
     * @return The next token in the sequence.
     */
    private YamlToken flow(YamlToken token) {
        pushed.add(token);

        // Handle sequence entries and determine their context
        if (!hasSequenceEntry && token != YamlToken.SEQUENCE_START && context() == YamlToken.SEQUENCE_START) {
            hasSequenceEntry = true;
            pushed.add(YamlToken.SEQUENCE_ENTRY);
        }
        contextPush(token, -1);

        // Update the flow depth to the context size
        if (flowDepth > contextSize())
            flowDepth = contextSize();
        return popPushed();
    }

    /**
     * Placeholder method to handle reserved YAML constructs.
     * Currently, this operation is unsupported.
     */
    private void readReserved() {
        throw new UnsupportedOperationException();
    }

    /**
     * Process and tokenize YAML directives.
     * Directives apply specific parsing rules or serve to transfer metadata.
     */
    private void readDirective() {
        readWords();
    }

    /**
     * Reads and processes a folded style in YAML.
     * Folded style treats newlines as spaces, preserving newlines only when followed by more newlines.
     */
    private void readFolded() {
        readLiteral(false);
    }

    /**
     * Obtain or initialize the temporary bytes buffer.
     *
     * @return The temporary buffer as {@link Bytes}.
     */
    private Bytes<?> temp() {
        if (temp == null)
            temp = Bytes.allocateElasticOnHeap(32);
        temp.clear();
        return temp;
    }

    /**
     * Read and process the literal block scalar style in YAML.
     * Literal style preserves newlines, treating them as part of the content.
     */
    private void readLiteral() {
        readLiteral(true);
    }

    /**
     * Reads and processes a literal scalar block from the YAML input.
     * In YAML, literal scalars are indicated by the pipe character '|'.
     * This method will capture content preserving formatting and any newlines
     * present, depending on the withNewLines flag.
     *
     * @param withNewLines A flag indicating if newlines should be preserved (true)
     * or converted to spaces (false) during the read process.
     */
    private void readLiteral(boolean withNewLines) {
        readNewline(); // read to the end of the line.
        readIndent();
        int indent2 = Math.toIntExact(in.readPosition() - lineStart);
        blockStart = blockEnd = -1;

        // Initialize or reset the temporary buffer
        final Bytes<?> temp = temp();
        long start = in.readPosition();

        // Process characters until reaching the end of the input
        while (true) {
            int ch = in.readUnsignedByte();
            if (ch < 0) {
                // Reached end of input, write any remaining content to temp buffer.
                long length = in.readPosition() - start;
                temp.write((BytesStore<?, ?>) in, start, length);
                break;
            }
            if (ch == '\r' || ch == '\n') {
                // Reached end of line, update buffer and handle indentation.
                unreadLast();
                if (withNewLines)
                    readNewline();
                long length = in.readPosition() - start;
                temp.write((BytesStore<?, ?>) in, start, length);

                readIndent();
                int indent3 = Math.toIntExact(in.readPosition() - lineStart);
                if (indent3 < indent2)
                    return;

                // If not preserving newlines, add space as separator if previous character isn't whitespace.
                if (!withNewLines)
                    if (temp.peekUnsignedByte(temp.writePosition() - 1) > ' ')
                        temp.append(' ');

                if (indent3 > indent2)
                    in.readPosition(lineStart + indent2);
                start = in.readPosition();
            }
        }
    }

    /**
     * Reads and processes the indentation of the current line.
     * Whitespace characters are consumed, and any newline characters
     * encountered will reset the lineStart marker.
     */
    private void readIndent() {
        while (true) {
            int ch = in.peekUnsignedByte();
            if (ch < 0 || ch > ' ')
                break;

            in.readSkip(1); // Consume the character.

            // If newline is encountered, update the lineStart marker.
            if (ch == '\r' || ch == '\n')
                lineStart = in.readPosition();
        }
    }

    /**
     * Consumes and processes newline characters from the input.
     * It will keep reading and updating the lineStart until it encounters a non-whitespace character or reaches end of input.
     */
    private void readNewline() {
        while (true) {
            int ch = in.peekUnsignedByte(); // Peek the next byte without consuming.

            // Break loop if end of input is reached or a non-whitespace character is encountered.
            if (ch < 0 || ch >= ' ')
                break;

            in.readSkip(1); // Consume the character.
            lineStart = in.readPosition(); // Update the lineStart marker.
        }
    }

    /**
     * Handles and determines the indentation level and relevant token type based on context.
     *
     * @param indented The token for the start of indentation context.
     * @param key The key token type.
     * @param push The token type to be pushed to the stack.
     * @param indent The current indentation level.
     * @return The next token after processing the current input.
     */
    private YamlToken indent(
            YamlToken indented,
            @NotNull YamlToken key,
            @NotNull YamlToken push,
            int indent) {
        if (push != YamlToken.STREAM_START)
            this.pushed.add(push);
        if (isInFlow()) {
            return key; // If we are inside a flow structure, return the key token.
        }
        int pos = this.pushed.size();

        // Pop contexts until the current indent matches the existing context.
        while (indent < contextIndent()) {
            contextPop();
        }
        int contextIndent = contextIndent();

        // Push the indented token if we are starting a new indentation level.
        if (indented != null && indent != contextIndent)
            this.pushed.add(indented);
        this.pushed.add(key);

        // Reverse the order of the tokens in the pushed stack.
        reversePushed(pos);

        // Push a new context if we are starting a new indentation level.
        if (indented != null && indent > contextIndent())
            contextPush(indented, indent);
        return popPushed();
    }

    /**
     * Reads plain scalar text from the YAML input, handling mappings and sequences.
     *
     * @param indent2 The current indentation level.
     * @return The token after processing the text.
     */
    private YamlToken readText(int indent2) {
        long pos = in.readPosition(); // Store the current position of input.

        blockQuote = 0;
        readWords(); // Read words until we reach a character that is not part of the scalar.

        // If we've reached the end of a field, determine if this is a key in a mapping.
        if (isFieldEnd()) {
            lastKeyPosition = pos;
            if (topContext().token != YamlToken.MAPPING_KEY)
                return indent(YamlToken.MAPPING_START, YamlToken.MAPPING_KEY, YamlToken.TEXT, indent2);
        }

        // By default, treat the scalar as plain text.
        YamlToken token = YamlToken.TEXT;
        return seq(token); // Handle sequences if needed.
    }

    /**
     * Handles the sequence logic within the YAML structure.
     *
     * @param token The current token being processed.
     * @return A {@link YamlToken} after processing the sequence logic.
     */
    private YamlToken seq(YamlToken token) {
        // If a sequence entry has not been processed yet and the current context is a sequence start, and it's in flow
        if (!hasSequenceEntry && context() == YamlToken.SEQUENCE_START && isInFlow()) {
            hasSequenceEntry = true; // Set that sequence entry has been processed.
            pushed.add(token);
            return YamlToken.SEQUENCE_ENTRY; // Return SEQUENCE_ENTRY token.
        }
        return token; // Otherwise, return the original token.
    }

    /**
     * Moves back by one position in the input stream.
     */
    private void unreadLast() {
        in.readSkip(-1); // Go back by one character.
    }

    /**
     * @return The current indentation level based on the top context or 0 if the context stack is empty.
     */
    private int contextIndent() {
        return contexts.isEmpty() ? 0 : topContext().indent; // Return the indent of the top context.
    }

    /**
     * Checks if the parser is inside a flow context.
     *
     * @return {@code true} if inside a flow context, {@code false} otherwise.
     */
    private boolean isInFlow() {
        return contextSize() >= flowDepth;
    }

    /**
     * Pops all contexts down to the specified level.
     *
     * @param downTo The level to which to pop the context.
     */
    void popAll(int downTo) {
        int pos = pushed.size();
        while (contextSize() > downTo) {
            contextPop();
        }
        reversePushed(pos); // Reverse the order of pushed tokens after popping.
    }

    /**
     * Reverses the order of tokens in the pushed list starting from the specified position.
     *
     * @param pos The starting position in the pushed list.
     */
    private void reversePushed(int pos) {
        for (int i = pos, j = pushed.size() - 1; i < j; i++, j--)
            pushed.set(i, pushed.set(j, pushed.get(i)));
    }

    /**
     * Retrieves and removes the last token from the pushed list or fetches the next token from the input if the list is empty.
     *
     * @return The retrieved {@link YamlToken}.
     */
    private YamlToken popPushed() {
        return pushed.isEmpty() ? next(Integer.MIN_VALUE) : pushed.remove(pushed.size() - 1);
    }

    /**
     * Reads a word from the input stream, handling quoted values.
     */
    private void readWord() {
        blockStart = in.readPosition(); // Mark the start of the word.
        boolean isQuote = in.peekUnsignedByte() == '<'; // Check if the word starts with a '<'.
        int ch = in.readUnsignedByte(); // Read the next character.
        do {
            // Check if the character is a special YAML character or whitespace.
            if (ch <= ' ' || (!isQuote && ",{}:?'\"#".indexOf(ch) >= 0)) {
                unreadLast(); // Move back if the character is special.
                break;
            }
            blockEnd = in.readPosition(); // Mark the end of the word.
            if (isQuote && ch == '>') {
                blockStart++;
                blockEnd--;
                break;
            }
            ch = in.readUnsignedByte(); // Read the next character.
        } while (ch != -1); // Continue until the end of input.
    }

    /**
     * Reads multiple words or tokens from the input stream, processing YAML special characters and structures.
     */
    private void readWords() {
        blockStart = in.readPosition(); // Mark the start position.
        while (in.readRemaining() > 0) { // Continue until the end of the input.
            int ch = in.readUnsignedByte(); // Read the next character.
            switch (ch) {
                case ':':
                    // If the character following ':' is not whitespace, treat it as part of the current word.
                    if (in.peekUnsignedByte() > ' ')
                        continue;
                    // is a field.
                    unreadLast();
                    return;
                case ',':
                    // If the current context is not a sequence start or a mapping start, treat ',' as part of the current word.
                    if (context() != YamlToken.SEQUENCE_START && context() != YamlToken.MAPPING_START)
                        continue;
                    unreadLast();
                    return;

                case '[': {
                    long pos = in.readPosition();
                    // If the character before '[' is not whitespace and the next character is ']', treat it as a special token.
                    if (in.peekUnsignedByte(pos - 2) > ' ' &&
                            in.peekUnsignedByte() == ']') {
                        in.readSkip(1); // Skip the next character.
                        blockEnd = pos + 1; // Mark the end position.
                        return;
                    }
                    unreadLast(); // Move back to the '[' character.
                    return;
                }
                case ']':
                case '{':
                case '}':
                case '#':
                case '\n':
                case '\r':
                    unreadLast();
                    return;
            }
            if (ch > ' ')
                blockEnd = in.readPosition();
        }
    }

    /**
     * Removes the top context from the context stack, and frees up the context.
     */
    private void contextPop() {
        YTContext context0 = contexts.remove(contextSize() - 1); // Remove the top context.
        // Reset the flow depth if it's greater than the current context size.
        if (flowDepth > contextSize())
            flowDepth = Integer.MAX_VALUE;
        YamlToken toEnd = context0.token.toEnd; // Get the ending token of the context.
        if (toEnd == null)
            throw new IllegalStateException("context: " + context0); // Throw an error if the context's ending token is null.
        // If the context has a valid ending token, add it to the pushed list.
        if (toEnd != YamlToken.NONE)
            pushed.add(toEnd);
        // Add the removed context to the list of free contexts, which can be reused in the future.
        freeContexts.add(context0);
    }

    /**
     * Reverts to a specified context level.
     *
     * @param contextSize The desired context level.
     */
    void revertToContext(int contextSize) {
        pushed.clear(); // Clear the pushed tokens.
        // Remove contexts until reaching the desired context size.
        while (contextSize() > contextSize) {
            YTContext context0 = contexts.remove(contextSize() - 1);
            if (flowDepth == contextSize())
                flowDepth = Integer.MAX_VALUE; // Reset the flow depth if required.
            freeContexts.add(context0); // Store the removed context for future reuse.
        }
    }

    /**
     * Pushes a new context to the context stack.
     *
     * @param context The YAML token representing the context.
     * @param indent  The indentation level for this context.
     */
    private void contextPush(YamlToken context, int indent) {
        // If we're at the start of a stream and the context isn't the end of directives,
        // we add an end of directives context before the actual context.
        if (context() == YamlToken.STREAM_START && context != YamlToken.DIRECTIVES_END) {
            pushContext0(YamlToken.DIRECTIVES_END, NO_INDENT);
            pushContext0(context, indent);
            push(YamlToken.DIRECTIVES_END);
            return;
        }
        pushContext0(context, indent);
    }

    /**
     * Reads a value enclosed in double quotes from the input stream.
     * Supports escape sequences.
     */
    private void readDoublyQuoted() {
        blockQuote = '"';
        blockStart = in.readPosition(); // Mark the start of the quoted string.
        // Continue reading until the end of the input.
        while (in.readRemaining() > 0) {
            int ch = in.readUnsignedByte();
            if (ch == '\\') {
                ch = in.readUnsignedByte(); // Handle escaped characters.
            } else if (ch == blockQuote) { // End quote found.
                blockEnd = in.readPosition() - 1;
                return;
            }
            // Throw an exception if the end of input is reached without finding the closing quote.
            if (ch < 0) {
                throw new IllegalStateException("Unterminated quotes " + in.subBytes(blockStart - 1, in.readPosition()));
            }
        }
    }

    /**
     * Reads a value enclosed in single quotes from the input stream.
     * Supports consecutive single quotes as escape for a single quote.
     */
    private void readSinglyQuoted() {
        blockQuote = '\'';
        blockStart = in.readPosition(); // Mark the start of the quoted string.
        // Continue reading until the end of the input.
        while (in.readRemaining() > 0) {
            int ch = in.readUnsignedByte();
            if (ch == blockQuote) {
                // ignore double single quotes.
                int ch2 = in.peekUnsignedByte();
                if (ch2 == blockQuote) { // Check for two consecutive single quotes (escaped quote).
                    in.readSkip(1);
                    continue;
                }
                blockEnd = in.readPosition() - 1; // End quote found.
                return;
            }
            // Throw an exception if the end of input is reached without finding the closing quote.
            if (ch < 0) {
                throw new IllegalStateException("Unterminated quotes " + in.subBytes(blockStart - 1, in.readPosition()));
            }
        }
    }

    /**
     * Checks if the current position in the stream marks the end of a field (denoted by a colon).
     *
     * @return true if the current position is a field end, false otherwise.
     */
    private boolean isFieldEnd() {
        consumeSpaces(); // Consume any spaces or tabs.
        // Check if the next character is a colon.
        if (in.peekUnsignedByte() == ':') {
            // Peek at the character after the colon.
            int ch = in.peekUnsignedByte(in.readPosition() + 1);
            // Skip 2 bytes if the next character is a tab or space, otherwise skip just the colon.
            in.readSkip((ch == '\t' || ch == ' ') ? 2 : 1);
            return true; // The colon signifies the end of a field.
        }
        return false;
    }

    /**
     * Reads a comment from the stream until the end of line or end of stream.
     */
    private void readComment() {
        consumeSpaces(); // Consume any spaces or tabs.
        blockStart = blockEnd = in.readPosition(); // Mark the start of the comment.
        while (true) {
            int ch = in.readUnsignedByte();
            if (ch < 0)
                return; // End of stream, break out.
            if (ch == '\n' || ch == '\r') { // New line or carriage return indicates end of comment.
                unreadLast(); // Move back the read position to the newline character.
                return;
            }
            if (ch > ' ')
                blockEnd = in.readPosition(); // Update the end of the comment block if a non-space character is found.
        }
    }

    /**
     * Consumes spaces and tabs from the stream.
     */
    private void consumeSpaces() {
        while (true) {
            int ch = in.peekUnsignedByte();
            if (ch == ' ' || ch == '\t') {
                in.readSkip(1); // Skip the space or tab.
            } else {
                return; // If not a space or tab, break out.
            }
        }
    }

    /**
     * Consumes all forms of whitespace including spaces, tabs, newlines, and carriage returns.
     */
    private void consumeWhitespace() {
        while (true) {
            int ch = in.peekUnsignedByte();
            if (ch >= 0 && ch <= ' ') { // Check for ASCII value of whitespace characters.
                in.readSkip(1); // Skip the whitespace character.
                // Update the line start position if a new line or carriage return is encountered.
                if (ch == '\n' || ch == '\r')
                    lineStart = in.readPosition();
            } else {
                return; // If not a whitespace character, break out.
            }
        }
    }

    /**
     * Gets the start position of the current line in the stream.
     *
     * @return the position of the start of the current line.
     */
    public long lineStart() {
        return lineStart;
    }

    /**
     * Sets the start position of the current line in the stream.
     *
     * @param lineStart the new starting position for the current line.
     */
    public void lineStart(long lineStart) {
        this.lineStart = lineStart;
    }

    /**
     * Gets the start position of the current block in the stream.
     *
     * @return the position of the start of the current block.
     */
    public long blockStart() {
        return blockStart;
    }

    /**
     * Gets the end position of the current block in the stream.
     *
     * @return the position of the end of the current block.
     */
    public long blockEnd() {
        return blockEnd;
    }

    /**
     * Pushes a new context onto the stack, or reuses one from the freeContexts list.
     *
     * @param token  The YAML token for the context.
     * @param indent The indentation level for the context.
     */
    private void pushContext0(YamlToken token, int indent) {
        YTContext context = freeContexts.isEmpty() ? new YTContext() : freeContexts.remove(freeContexts.size() - 1);
        context.token = token;
        context.indent = indent;
        if (context.keys != null)
            context.keys.reset(); // Reset the keys if they exist.
        contexts.add(context); // Add the new context to the list.
    }

    @Override
    public String toString() {
        String name = last.name();
        return last + " " + (blockQuote == 0 || name.endsWith("_START") || name.endsWith("_END") ? "" : blockQuote + " ") + text();
    }

    /**
     * Gets the current block's quotation character.
     *
     * @return the quotation character used in the current block, or 0 if none.
     */
    public char blockQuote() {
        return blockQuote;
    }

    /**
     * Used primarily for testing purposes to extract the current block's text.
     *
     * @return the text of the current block or an empty string if no text.
     */
    public String text() {
        try (ScopedResource<StringBuilder> sbTl = SBP.get()) {
            final StringBuilder sb = sbTl.get();
            text(sb);
            return sb.length() == 0 ? "" : sb.toString();
        }
    }

    /**
     * Extracts the text of the current block into the provided StringBuilder.
     *
     * @param sb StringBuilder to which the block's text will be appended.
     */
    public void text(StringBuilder sb) {
        // If blockEnd is not set and a temporary value exists, use that.
        if (blockEnd < 0 && temp != null) {
            sb.append(temp);
            return;
        }
        sb.setLength(0);  // Clear the StringBuilder.

        // Return if there is no text to parse or if last token doesn't allow text extraction.
        if (blockStart == blockEnd || NO_TEXT.contains(last))
            return;
        long pos = in.readPosition();
        try {
            in.readPosition(blockStart);
            in.parseUtf8(sb, Math.toIntExact(blockEnd - blockStart));
        } finally {
            // Reset the reading position.
            in.readPosition(pos);
        }
    }

    /**
     * Parses the current block's content as a double.
     *
     * @return the parsed double value.
     */
    public double parseDouble() {
        if (blockEnd < 0 && temp != null) {
            return temp.parseDouble();
        }
        if (blockStart == blockEnd || NO_TEXT.contains(last))
            return -0.0;  // Return -0.0 if there's no data.

        long pos = in.readPosition();
        try {
            in.readPosition(blockStart);
            return in.parseDouble();
        } finally {
            // Reset the reading position.
            in.readPosition(pos);
        }
    }

    /**
     * Parses the current block's content as a long. Handles octal numbers as well.
     *
     * @return the parsed long value.
     */
    public long parseLong() {
        if (blockEnd < 0 && temp != null) {
            return temp.parseLong();
        }
        if (blockStart == blockEnd || NO_TEXT.contains(last))
            return 0;  // Return 0 if there's no data.

        long pos = in.readPosition();
        try {
            in.readPosition(blockStart);
            if (in.peekUnsignedByte() == '0') {
                // Handle octal numbers.
                final int i = in.peekUnsignedByte(in.readPosition() + 1);
                try (final ScopedResource<StringBuilder> sbTl = SBP.get()) {
                    StringBuilder sb = sbTl.get();
                    if (Character.isDigit(i)) {
                        in.readSkip(1);
                        in.parseUtf8(sb, Math.toIntExact(blockEnd - blockStart) - 1);
                    return Long.parseLong(sb.toString(), 8);  // Parse as octal.
                    } else if (i == 'o') {
                        in.readSkip(2);
                        in.parseUtf8(sb, Math.toIntExact(blockEnd - blockStart) - 2);
                    return Long.parseLong(sb.toString(), 8);  // Parse as octal.
                    }
                }
            }
            return in.parseLong();
        } finally {
            // Reset the reading position.
            in.readPosition(pos);
        }
    }

    /**
     * Adds a YamlToken to the list of pushed tokens.
     *
     * @param token The YamlToken to be pushed.
     */
    public void push(YamlToken token) {
        pushed.add(token);
    }

    /**
     * Checks if the text of the current block is equal to the provided string.
     *
     * @param s The string to be checked.
     * @return true if the text of the current block is equal to 's', false otherwise.
     */
    public boolean isText(String s) {
        // TODO: This method can potentially be optimized for efficiency.
        return text().equals(s);
    }

    /**
     * Retrieves the YamlKeys associated with the top context. If none exist, a new YamlKeys object is created.
     *
     * @return The YamlKeys associated with the top context.
     */
    public YamlKeys keys() {
        YTContext context = topContext();
        YamlKeys key = context.keys;
        if (key == null)
            return context.keys = new YamlKeys();
        return key;
    }

    /**
     * Resets the reading to start from the specified offset.
     *
     * @param offset The position from which the reading should start.
     */
    public void rereadFrom(long offset) {
        lineStart = offset;
        pushed.clear();
    }

    /**
     * Inner static class that represents the context during YAML parsing.
     * This can include the current token, the indent level, and associated keys.
     */
    static class YTContext extends SelfDescribingMarshallable {
        YamlToken token;     // The current token in this context.
        int indent;          // Indentation level of this context.
        YamlKeys keys = null;  // YamlKeys associated with this context.

        /**
         * Gets the count of keys in this context.
         *
         * @return The number of keys, or 0 if none exist.
         */
        int keysCount() {
            return keys == null ? 0 : keys.count;
        }
    }
}
