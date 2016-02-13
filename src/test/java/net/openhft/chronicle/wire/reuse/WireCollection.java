package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.core.annotation.NotNull;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import net.openhft.chronicle.wire.WireType;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author gadei
 */
public class WireCollection extends WireModel implements Marshallable {

    private String reference;
    private String path;
    private String name;
    private Map<String, WireProperty> properties = new HashMap<>();
    private Map<String, WireCollection> collections = new HashMap<>();

    public WireCollection() {
    }

    public WireCollection(String reference, String path, String name, long id, int revision, String key) {
        super(id, revision, key);
        this.reference = reference;
        this.path = path;
        this.name = name;
    }

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IllegalStateException {
        super.readMarshallable(wire);
        this.reference = wire.read(WireModelValues.reference).text();
        this.path = wire.read(WireModelValues.path).text();
        this.name = wire.read(WireModelValues.name).text();
        this.properties = wire.read(WireModelValues.properties).marshallableAsMap(WireProperty.class);
        this.collections = wire.read(WireModelValues.collections).marshallableAsMap(WireCollection.class);
    }

    @Override
    public void writeMarshallable(WireOut wire) {
        super.writeMarshallable(wire);
        wire
                .write(WireModelValues.reference).text(reference)
                .write(WireModelValues.path).text(path)
                .write(WireModelValues.name).text(name);
        if (properties.size() > 0) {
            wire.write(WireModelValues.properties).marshallableAsMap(properties);
        }
        if (collections.size() > 0) {
            wire.write(WireModelValues.collections).marshallableAsMap(collections);
        }
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

    public void clearProperties() {
        properties.clear();
    }

    public Map<String, WireProperty> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, WireProperty> properties) {
        this.properties = properties;
    }

    public void addProperty(WireProperty property) {
        this.properties.put(property.getReference(), property);
    }

    public Map<String, WireCollection> getCollections() {
        return collections;
    }

    public void setCollections(Map<String, WireCollection> collections) {
        this.collections = collections;
    }

    public void addCollection(WireCollection collection) {
        this.collections.put(collection.getReference(), collection);
    }

    @Override
    public String toString() {
        return WireType.TEXT.asString(this);
    }
}
