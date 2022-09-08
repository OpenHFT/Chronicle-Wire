package software.chronicle.demo.wire.channelArith;

import net.openhft.chronicle.wire.Marshallable;

public interface CalcHandler extends ArithListener {

    public Calculator calculator ( ArithListener expr );

}
