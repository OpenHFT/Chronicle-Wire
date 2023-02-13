package net.openhft.chronicle.wire.observable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.observable.Observable;
import net.openhft.chronicle.core.observable.StateReporter;
import net.openhft.chronicle.wire.YamlWire;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WireStateReporterTest {

    private WireStateReporter wireStateReporter;
    private YamlWire wire;

    @BeforeEach
    void setUp() {
        wire = new YamlWire(Bytes.allocateElasticOnHeap());
        wireStateReporter = new WireStateReporter(wire);
    }

    @Test
    public void willDumpASingleThing() {
        wireStateReporter.writeChild("root", new ObservableTree("hello"));
        assertEquals("root: {\n" +
                "  _id: \"ObservableTree:hello\",\n" +
                "  name: hello\n" +
                "}\n", wire.toString());
    }

    @Test
    void willDumpATreeOfThings() {
        final ObservableTree one = new ObservableTree("one");
        final ObservableTree two = new ObservableTree("two");
        final ObservableTree three = new ObservableTree("three");
        final ObservableTree four = new ObservableTree("four");
        one.addChild("two", two);
        one.addChild("three", three);
        three.addChild("four", four);
        wireStateReporter.writeChild("root", one);
        assertEquals("root: {\n" +
                "  _id: \"ObservableTree:one\",\n" +
                "  name: one,\n" +
                "  two: {\n" +
                "    _id: \"ObservableTree:two\",\n" +
                "    name: two\n" +
                "  },\n" +
                "  three: {\n" +
                "    _id: \"ObservableTree:three\",\n" +
                "    name: three,\n" +
                "    four: { _id: \"ObservableTree:four\", name: four }\n" +
                "  }\n" +
                "}\n", wire.toString());
    }

    @Test
    void willDumpACollectionOfThings() {
        final ObservableTree one = new ObservableTree("one");
        final ObservableTree two = new ObservableTree("two");
        final ObservableTree three = new ObservableTree("three");
        final ObservableTree four = new ObservableTree("four");
        one.addListOfChildren(two, three, four);
        wireStateReporter.writeChild("root", one);
        assertEquals("root: {\n" +
                "  _id: \"ObservableTree:one\",\n" +
                "  name: one,\n" +
                "  listOfChildren: [\n" +
                "    { _id: \"ObservableTree:two\", name: two },\n" +
                "    { _id: \"ObservableTree:three\", name: three },\n" +
                "    { _id: \"ObservableTree:four\", name: four }\n" +
                "  ]\n" +
                "}\n", wire.toString());
    }

    @Test
    void willOnlyPrintElementsTheFirstTimeTheyAreEncountered() {
        final ObservableTree one = new ObservableTree("one");
        final ObservableTree two = new ObservableTree("two");
        final ObservableTree three = new ObservableTree("three");
        final ObservableTree four = new ObservableTree("four");
        final ObservableTree five = new ObservableTree("five");
        one.addChild("two", two);
        two.addChild("one", one); // a loop
        one.addChild("three", three);
        three.addChild("four", four);
        three.addChild("two", two); // second appearance of two
        one.addListOfChildren(one, two, three, four, five, five);
        wireStateReporter.writeChild("root", one);
        assertEquals("root: {\n" +
                "  _id: \"ObservableTree:one\",\n" +
                "  name: one,\n" +
                "  two: {\n" +
                "    _id: \"ObservableTree:two\",\n" +
                "    name: two,\n" +
                "    one: { _id: \"ObservableTree:one\" }\n" +
                "  },\n" +
                "  three: {\n" +
                "    _id: \"ObservableTree:three\",\n" +
                "    name: three,\n" +
                "    four: { _id: \"ObservableTree:four\", name: four },\n" +
                "    two: { _id: \"ObservableTree:two\" }\n" +
                "  },\n" +
                "  listOfChildren: [\n" +
                "    { _id: \"ObservableTree:one\" },\n" +
                "    { _id: \"ObservableTree:two\" },\n" +
                "    { _id: \"ObservableTree:three\" },\n" +
                "    { _id: \"ObservableTree:four\" },\n" +
                "    { _id: \"ObservableTree:five\", name: five },\n" +
                "    { _id: \"ObservableTree:five\" }\n" +
                "  ]\n" +
                "}\n", wire.toString());
    }

    private static class ObservableTree implements Observable {

        private final String name;
        private final Map<String, ObservableTree> children;
        private final List<ObservableTree> listOfChildren;

        public ObservableTree(String name) {
            this.name = name;
            this.children = new HashMap<>();
            this.listOfChildren = new ArrayList<>();
        }

        public void addChild(String childName, ObservableTree child) {
            this.children.put(childName, child);
        }

        public void addListOfChildren(ObservableTree... children) {
            listOfChildren.addAll(Arrays.asList(children));
        }

        @Override
        public void dumpState(StateReporter stateReporter) {
            stateReporter.writeProperty("name", name);
            if (!children.isEmpty())
                children.entrySet().forEach(e -> stateReporter.writeChild(e.getKey(), e.getValue()));
            if (!listOfChildren.isEmpty()) {
                stateReporter.writeChildren("listOfChildren", listOfChildren);
            }
        }

        @Override
        public String idString() {
            return "ObservableTree:" + name;
        }
    }
}