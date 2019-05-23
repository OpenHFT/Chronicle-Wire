package net.openhft.chronicle.wire;

interface CommentAnnotationNotifier {
    void hasPrecedingComment(boolean hasCommentAnnotation);
}