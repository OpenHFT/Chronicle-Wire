package net.openhft.chronicle.wire;

@Deprecated
public interface SharedDocumentContext {
    <T extends SharedDocumentContext> T documentContext(ThreadLocal<DocumentContextHolder> documentContext);
}
