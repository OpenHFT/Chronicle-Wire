package net.openhft.chronicle.wire;

import java.util.function.Supplier;

/**
 * Created by peter on 15/01/15.
 */
public class TextWire implements Wire {
    @Override
    public WriteValue write() {
        return null;
    }

    @Override
    public WriteValue write(WireKey key) {
        return null;
    }

    @Override
    public WriteValue write(CharSequence name, WireKey template) {
        return null;
    }

    @Override
    public ReadValue read() {
        return null;
    }

    @Override
    public ReadValue read(WireKey key) {
        return null;
    }

    @Override
    public ReadValue read(Supplier<StringBuilder> name, WireKey template) {
        return null;
    }

    @Override
    public boolean hasNextSequenceItem() {
        return false;
    }

    @Override
    public void readSequenceEnd() {

    }

    @Override
    public void writeComment(CharSequence s) {

    }

    @Override
    public void readComment(StringBuilder sb) {

    }

    @Override
    public boolean hasMapping() {
        return false;
    }

    @Override
    public void writeDocumentStart() {

    }

    @Override
    public void writeDocumentEnd() {

    }

    @Override
    public boolean hasDocument() {
        return false;
    }

    @Override
    public void readDocumentStart() {

    }

    @Override
    public void readDocumentEnd() {

    }

    @Override
    public void flip() {

    }

    @Override
    public void clear() {

    }
}
