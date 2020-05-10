package net.openhft.chronicle.wire;

public interface SharedDocumentContext {
    <T extends SharedDocumentContext> T documentContext(ThreadLocal<DocumentContextHolder> documentContext);
}
