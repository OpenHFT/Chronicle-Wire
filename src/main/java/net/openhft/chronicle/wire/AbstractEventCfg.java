package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;

public class AbstractEventCfg<E extends AbstractEventCfg<E>> extends AbstractMarshallableCfg implements Event<E> {
    private String eventId = "";
    @LongConversion(ServicesTimestampLongConverter.class)
    private long eventTime;
    private String serviceId = "";


    @Override
    public void readMarshallable(WireIn wireIn) throws IORuntimeException {
        Wires.readMarshallable(this, wireIn, true);
    }

    @NotNull
    @Override
    public String eventId() {
        return eventId;
    }

    @Override
    public E eventId(CharSequence eventId) {
        this.eventId = eventId.toString();
        return (E) this;
    }

    @Override
    public long eventTime() {
        return eventTime;
    }

    @Override
    public E eventTime(long eventTime) {
        this.eventTime = eventTime;
        return (E) this;
    }

    @Override
    public E eventTimeNow() {
        return this.eventTime(ServicesTimestampLongConverter.currentTime());
    }

    @NotNull
    public String serviceId() {
        return serviceId;
    }

    public E serviceId(String serviceId) {
        this.serviceId = serviceId;
        return (E) this;
    }
}
