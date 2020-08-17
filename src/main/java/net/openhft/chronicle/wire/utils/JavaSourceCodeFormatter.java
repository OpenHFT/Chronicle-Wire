package net.openhft.chronicle.wire.utils;

import java.util.concurrent.atomic.AtomicInteger;

public class JavaSourceCodeFormatter extends SourceCodeFormatter {
    private static final int INDENT_SPACES = 4;

    public JavaSourceCodeFormatter() {
        super(INDENT_SPACES);
    }

    public JavaSourceCodeFormatter(int indent) {
        super(INDENT_SPACES, indent);
    }

    public JavaSourceCodeFormatter(AtomicInteger indent) {
        super(INDENT_SPACES, indent);
    }
}
