package net.openhft.chronicle.wire;

interface CommentAnnotationNotifier {
    void hasPreseedingComment(boolean hasCommentAnnotation);

    boolean canProvideComment();
}