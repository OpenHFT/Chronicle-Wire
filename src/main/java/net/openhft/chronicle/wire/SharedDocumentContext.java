package net.openhft.chronicle.wire;

@Deprecated(/* to be removed in x.22 */)
public interface SharedDocumentContext {
    <T extends SharedDocumentContext> T documentContext(ThreadLocal<DocumentContextHolder> documentContext);
}
