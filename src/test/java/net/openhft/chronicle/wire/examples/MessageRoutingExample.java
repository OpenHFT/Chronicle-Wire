package net.openhft.chronicle.wire.examples;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.*;

import java.util.HashMap;
import java.util.Map;

import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;

public class MessageRoutingExample {

    static {
        // used to allow for the objects in the streamed data to take the simple name rather than the full path name of the class
        CLASS_ALIASES.addAlias(Product.class, ProductHandler.class);
    }

    /**
     * used to route products to the destination by providing the appropriate ProductHandler
     */
    interface Routing {
        ProductHandler to(String destination);
    }


    /**
     * Implement this to read the process of the product events.
     */
    interface ProductHandler {
        void product(Product product);
    }

    /**
     * A example of a simple business event
     */
    static class Product extends SelfDescribingMarshallable {
        String name;

        public Product(String name) {
            this.name = name;
        }
    }


    public static void main(String[] args) {
        new MessageRoutingExample().demo();
    }

    // the serialized data gets written to 'wire'
    private final Wire wire = new TextWire(Bytes.allocateElasticOnHeap());

    private void demo() {

        final Map<String, ProductHandler> destinationMap = new HashMap<>();

        // add ProductHandler to handle messsages routed to each destination
        destinationMap.put("Italy", product -> System.out.println("Sends the product to Italy, product=" + product));
        destinationMap.put("France", product -> System.out.println("Sends the product to France, product=" + product));
        destinationMap.put("Russia", product -> System.out.println("Sends the product to Russia, product=" + product));

        final Routing routing = wire.methodWriter(Routing.class);
        routing.to("Italy").product(new Product("Coffee"));
        routing.to("France").product(new Product("Cheese"));
        routing.to("Russia").product(new Product("Vodka"));

        wire.methodReader((Routing) destinationMap::get).readOne();
    }

}

