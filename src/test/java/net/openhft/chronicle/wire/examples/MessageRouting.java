package net.openhft.chronicle.wire.examples;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.*;

import java.util.HashMap;
import java.util.Map;

public class MessageRouting {

    interface Routing {
        ProductHandler to(String destination);
    }

    static class Product extends SelfDescribingMarshallable {
        String name;

        public Product(String name) {
            this.name = name;
        }
    }


    interface ProductHandler {
        void product(Product product);
    }


    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(Product.class, ProductHandler.class);
    }


    public static void main(String[] args) {
        new MessageRouting().demo();
    }

    private final Wire wire = new TextWire(Bytes.allocateElasticOnHeap());

    private void demo() {


        final Map<String, ProductHandler> destinationMap = new HashMap<>();
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

