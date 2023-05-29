/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.bytes.DistributedUniqueTimeProvider;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import net.openhft.chronicle.core.time.TimeProvider;
import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.NanoTimestampLongConverter;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
/**
 * Class representing a context for a running system. It provides various system-related information,
 * such as the number of available processors, host ID and name, the time the system has been up,
 * the country and name of the user, and the Java vendor and version.
 * <p>
 * This context is implemented as a singleton using the INSTANCE constant. However, a SystemContext deserialized from another host would have different information.
 */
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
    private String javaVendor;
    private String javaVersion;

    /**
     * Retrieves the singleton instance of SystemContext, creating it if necessary.
     */
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
        sc.javaVendor = System.getProperty("java.vendor");
        sc.javaVersion = System.getProperty("java.version");
        return sc;
    }

    /**
     * Returns the number of available processors in the system.
     *
     * @return the number of available processors.
     */
    public int availableProcessors() {
        return availableProcessors;
    }

    /**
     * Returns the host ID of the system.
     *
     * @return the host ID.
     */
    public int hostId() {
        return hostId;
    }

    /**
     * Returns the host name of the system.
     *
     * @return the host name.
     */
    public String hostName() {
        return hostName;
    }

    /**
     * Returns the Java vendor of the system.
     *
     * @return the Java vendor.
     */
    public String javaVendor() {
        return javaVendor;
    }

    /**
     * Returns the Java version of the system.
     *
     * @return the Java version.
     */
    public String javaVersion() {
        return javaVersion;
    }

    /**
     * Returns the time (in nanoseconds) that the system has been up.
     *
     * @return the up time.
     */
    public long upTime() {
        return upTime;
    }

    /**
     * Returns the user's country on the system.
     *
     * @return the user's country.
     */
    public String userCountry() {
        return userCountry;
    }

    /**
     * Returns the username of the system user.
     *
     * @return the username.
     */
    public String userName() {
        return userName;
    }
}
