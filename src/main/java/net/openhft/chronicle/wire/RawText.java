package net.openhft.chronicle.wire;

class RawText {
    String text;

    public RawText(CharSequence text) {
        this.text = text.toString();
    }
}
