package net.openhft.chronicle.wire.observable;

import net.openhft.chronicle.core.observable.Observable;
import net.openhft.chronicle.core.observable.StateReporter;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireOut;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Stack;

public class WireStateReporter implements StateReporter {
    public static final String ID_PROPERTY_NAME = "_id";
    private final Wire wire;
    private final IdentityHashMap<Object, Object> encounteredObjects;
    private final Stack<WireOut> stack;

    public WireStateReporter(Wire wire) {
        this.wire = wire;
        encounteredObjects = new IdentityHashMap<>();
        stack = new Stack<>();
        stack.push(wire);
    }

    @Override
    public void writeProperty(String name, CharSequence value) {
        stack.peek().write(name).text(value);
    }

    @Override
    public void writeChild(String name, Object value) {
        wire.write(name).marshallable(wire -> {
            if (!encounteredObjects.containsKey(value)) {
                encounteredObjects.put(value, value);
                stack.push(wire);
                dumpFullObject(value, wire);
                stack.pop();
            } else {
                dumpReferenceOnly(value, wire);
            }
        });
    }

    @Override
    public void writeChildren(String name, Collection<?> children) {
        wire.write(name).sequence(children, (childrenInner, valueOut) -> {
            childrenInner.forEach(child -> valueOut.marshallable(wireOut -> {
                if (!encounteredObjects.containsKey(child)) {
                    encounteredObjects.put(child, child);
                    stack.push(wireOut);
                    dumpFullObject(child, wireOut);
                    stack.pop();
                } else {
                    dumpReferenceOnly(child, wireOut);
                }
            }));
        });
    }

    private void dumpReferenceOnly(Object child, WireOut wireOut) {
        if (child instanceof Observable) {
            Observable observable = (Observable) child;
            wireOut.write(ID_PROPERTY_NAME).text(observable.idString());
        } else {
            wireOut.write(ID_PROPERTY_NAME).text(child.getClass().getName() + "@" + System.identityHashCode(child));
        }
    }

    private void dumpFullObject(Object child, WireOut wireOut) {
        dumpReferenceOnly(child, wireOut);
        if (child instanceof Observable) {
            ((Observable) child).dumpState(this);
        } else {
            wireOut.write("toString").object(toString());
        }
    }
}
