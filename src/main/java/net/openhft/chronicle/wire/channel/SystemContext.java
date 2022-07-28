package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.bytes.DistributedUniqueTimeProvider;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import net.openhft.chronicle.core.time.TimeProvider;
import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.NanoTimestampLongConverter;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;

@SuppressWarnings("unused")
public class SystemContext extends SelfDescribingMarshallable {
    public static final SystemContext INSTANCE = getInstance();
    private int availableProcessors;
    private int hostId;
    private String hostName;
    @LongConversion(NanoTimestampLongConverter.class)
    private long upTime;
    private String userCountry;
    private String userName;

    private static SystemContext getInstance() {
        SystemContext sc = new SystemContext();
        final Runtime runtime = Runtime.getRuntime();
        sc.availableProcessors = runtime.availableProcessors();
        sc.hostId = Integer.getInteger("hostId", 0);
        sc.hostName = OS.getHostName();
        final TimeProvider tp = (sc.hostId == 0 ? SystemTimeProvider.INSTANCE : DistributedUniqueTimeProvider.instance());
        sc.upTime = tp.currentTimeNanos();
        sc.userCountry = System.getProperty("user.country");
        sc.userName = OS.getUserName();
        return sc;
    }

    public int availableProcessors() {
        return availableProcessors;
    }

    public int hostId() {
        return hostId;
    }

    public String hostName() {
        return hostName;
    }

    public long upTime() {
        return upTime;
    }

    public String userCountry() {
        return userCountry;
    }

    public String userName() {
        return userName;
    }
}
