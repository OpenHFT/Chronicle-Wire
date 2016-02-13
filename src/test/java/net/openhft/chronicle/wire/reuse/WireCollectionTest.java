package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import net.openhft.chronicle.wire.Wires;

import org.junit.Before;

/**
 * Created by peter.lawrey on 01/02/2016.
 */
@RunWith(value = Parameterized.class)
public class WireCollectionTest {

    private WireCollection collection;// = new WireModel();

    private final Function<Bytes, Wire> wireType;

    public WireCollectionTest(Function<Bytes, Wire> wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        return Arrays.asList(
                new Object[]{(Function<Bytes, Wire>) bytes -> new BinaryWire(bytes, false, true, false, 128, "binary")},
                new Object[]{WireType.TEXT},
                new Object[]{WireType.BINARY},
                new Object[]{WireType.FIELDLESS_BINARY},
                new Object[]{WireType.JSON}
        );
    }

    @Before
    public void setUp() {
        collection = WireUtils.randomWireCollection();
    }
    
    @Test
    public void testMultipleReads() {
        Bytes bytes = Bytes.elasticByteBuffer();
        Wire wire = wireType.apply(bytes);

        wire.writeDocument(true, collection);
        System.out.println(Wires.fromSizePrefixedBlobs(bytes));

        WireCollection results = new WireCollection();
        wire.readDocument(results, null);

        WireUtils.compareWireCollection(collection, results);
    }
}
