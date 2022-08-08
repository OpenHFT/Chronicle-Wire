/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under Contract only
 */

package net.openhft.chronicle.wire.channel.echo;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.channel.ChronicleChannel;

public abstract class ChannelVisitor<T> extends SelfDescribingMarshallable {
    public abstract T visit(ChronicleChannel channel);
}
