package software.chronicle.demo.wire.channelArith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.chronicle.demo.wire.channelDemo2.MessageListener;

public class Calculator implements CalcHandler {
    private static Logger LOGGER = LoggerFactory.getLogger(CalcHandler.class);

    private ArithListener arithListener;

    public Calculator calculator( ArithListener arithListener ) {
        this.arithListener = arithListener;
        return this;
    }

    public void calculate(ArithExpr expr) {
        LOGGER.info("Calculating {}", expr.toString());
        arithListener.calculate(expr.calculate());
    }
}
