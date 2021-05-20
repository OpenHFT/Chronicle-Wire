package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.UpdateInterceptor;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class MethodWriter2Test {

    static {
       // System.setProperty("dumpCode", "true");
    }

    @Test
    public void allowThrough() {
        check(true, ARGUMENT.DTO);
    }

    @Test
    public void allowThroughPrimitive() {
        check(true, ARGUMENT.PRIMITIVE);
    }

    @Test
    public void allowThroughNoArg() {
        check(true, ARGUMENT.NONE);
    }

    @Test
    public void block() {
        check(false, ARGUMENT.DTO);
    }

    @Test
    public void blockPrimitive() {
        check(false, ARGUMENT.PRIMITIVE);
    }

    @Test
    public void blockNoArg() {
        check(false, ARGUMENT.NONE);
    }

    private void check(boolean allowThrough, ARGUMENT argument) {
        Wire wire = WireType.BINARY.apply(Bytes.allocateElasticOnHeap());
        wire.usePadding(true);

        // checks that no exceptions are thrown here
        UpdateInterceptor ui = (methodName, t) -> allowThrough;
        FundingListener fundingListener = wire.methodWriterBuilder(FundingOut.class).updateInterceptor(ui).build();
        argument.accept(fundingListener);

        List<String> output = new ArrayList<>();
        FundingListener listener = Mocker.intercepting(FundingListener.class, "", output::add);
        @NotNull MethodReader mr = wire.methodReader(listener);

        if (allowThrough) {
            assertTrue(mr.readOne());
            assertEquals(1, output.size());
            assertEquals(argument.expected(), output.toString());
            assertFalse(mr.readOne());
        } else {
            assertFalse(mr.readOne());
            assertEquals(0, output.size());
        }
    }

    enum ARGUMENT implements Consumer<FundingListener> {
        DTO {
            @Override
            public String expected() {
                return "[funding[!net.openhft.chronicle.wire.method.Funding {\n" +
                        "  symbol: 0,\n" +
                        "  fr: NaN,\n" +
                        "  mins: 0\n" +
                        "}\n" +
                        "]]";
            }

            @Override
            public void accept(FundingListener fundingListener) {
                fundingListener.funding(new Funding());
            }
        },
        PRIMITIVE {
            @Override
            public String expected() {
                return "[fundingPrimitive[42]]";
            }

            @Override
            public void accept(FundingListener fundingListener) {
                fundingListener.fundingPrimitive(42);
            }
        },
        NONE {
            @Override
            public String expected() {
                return "[fundingNoArg[]]";
            }

            @Override
            public void accept(FundingListener fundingListener) {
                fundingListener.fundingNoArg();
            }
        };

        public abstract String expected();
    }
}
