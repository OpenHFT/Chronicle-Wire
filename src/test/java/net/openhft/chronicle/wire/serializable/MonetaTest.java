package net.openhft.chronicle.wire.serializable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import org.junit.Test;

import javax.money.CurrencyQueryBuilder;
import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class MonetaTest {
    @Test
    public void monetary() {
        Collection<CurrencyUnit> allCurrencies = Monetary.getCurrencies(CurrencyQueryBuilder.of().build());
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap());
        wire.write("currencies")
                .object(allCurrencies);
//        System.out.println(wire);
        Collection<CurrencyUnit> allCurrencies2 = wire.read("currencies").object(Collection.class);
        assertEquals(allCurrencies, allCurrencies2);
    }
}
