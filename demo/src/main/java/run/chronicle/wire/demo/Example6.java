package run.chronicle.wire.demo;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.Wires;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Write a message with a sequence of records.
 */
public class Example6 {
    public static void main(String[] args) {

        // Bytes which wraps a ByteBuffer which is resized as needed
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

        Wire wire = new TextWire(bytes);

        ClassAliasPool.CLASS_ALIASES.addAlias(Data.class);

        Data[] data = {
                new Data("Hello World", 98765, TimeUnit.HOURS, 1.5),
                new Data("G'Day All", 1212121, TimeUnit.MINUTES, 12.34),
                new Data("Howyall", 1234567890L, TimeUnit.SECONDS, 1000)
        };
        wire.writeDocument(false, w -> w.write("mydata")
                .sequence(v -> Stream.of(data).forEach(v::object)));

        //Prints out:
/*      --- !!data
        mydata: [
        !Data {
          message: Hello World,
          number: 98765,
          timeUnit: HOURS,
          price: 1.5
        },
        !Data {
          message: G'Day All,
          number: 1212121,
          timeUnit: MINUTES,
          price: 12.34
        },
        !Data {
          message: Howyall,
          number: 1234567890,
          timeUnit: SECONDS,
          price: 1E3
        }
      ]
 */
        System.out.println(Wires.fromSizePrefixedBlobs(bytes));

        List<Data> dataList = new ArrayList<>();
        wire.readDocument(null, w -> w.read("mydata")
                .sequence(dataList, (l, v) -> {
                    while (v.hasNextSequenceItem())
                        l.add(v.object(Data.class));
                }));


        //Prints out:
/*      Data{message='Hello World', number=98765, timeUnit=HOURS, price=1.5}
        Data{message='G'Day All', number=1212121, timeUnit=MINUTES, price=12.34}
        Data{message='Howyall', number=1234567890, timeUnit=SECONDS, price=1000.0}      */
        dataList.forEach(System.out::println);

//     To write in binary instead:

        Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
        Wire wire2 = new BinaryWire(bytes2);
        wire2.writeDocument(false, w -> w.write("mydata")
                .sequence(v -> Stream.of(data).forEach(v::object)));

        //Prints out
/*      --- !!data #binary
        mydata: [
          !Data {
            message: Hello World,
            number: !int 98765,
            timeUnit: HOURS,
            price: 1.5
          },
          !Data {
            message: G'Day All,
            number: !int 1212121,
            timeUnit: MINUTES,
            price: 12.34
          },
          !Data {
            message: Howyall,
            number: !int 1234567890,
            timeUnit: SECONDS,
            price: !int 1000
          }
        ]
 */
        System.out.println(Wires.fromSizePrefixedBlobs(bytes2));

        List<Data> dataList2 = new ArrayList<>();
        wire2.readDocument(null, w -> w.read("mydata")
                .sequence(dataList2, (l, v) -> {
                    while (v.hasNextSequenceItem())
                        l.add(v.object(Data.class));
                }));

        //Prints out:
/*      Data{message='Hello World', number=98765, timeUnit=HOURS, price=1.5}
        Data{message='G'Day All', number=1212121, timeUnit=MINUTES, price=12.34}
        Data{message='Howyall', number=1234567890, timeUnit=SECONDS, price=1000.0}      */
        dataList2.forEach(System.out::println);

        bytes.releaseLast();
        bytes2.releaseLast();
    }
}