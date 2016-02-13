package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.core.annotation.NotNull;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireKey;
import net.openhft.chronicle.wire.WireOut;

/**
 *
 * @author Gadi Eichhorn
 */
public class WireProperty extends WireModel implements Marshallable {

    private String reference;
    ;
    private String path;
    private String name;
    private String value;
    public WireProperty() {
    }

    public WireProperty(String reference, String path, String name, String value, long id, int revision, String key) {
        super(id, revision, key);
        this.reference = reference;
        this.path = path;
        this.name = name;
        this.value = value;
    }

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IllegalStateException {
        super.readMarshallable(wire);
        this.reference = wire.read(WireModelValues.reference).text();
        this.path = wire.read(WireModelValues.path).text();
        this.name = wire.read(WireModelValues.name).text();
        this.value = wire.read(WireModelValues.value).text();
    }

    @Override
    public void writeMarshallable(WireOut wire) {
        super.writeMarshallable(wire);
        wire
                .write(WireModelValues.reference).text(reference)
                .write(WireModelValues.path).text(path)
                .write(WireModelValues.name).text(name)
                .write(WireModelValues.value).text(value);
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    enum Values implements WireKey {
    }

}
