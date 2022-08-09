/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under Contract only
 */

package net.openhft.chronicle.wire.channel.echo;

public interface ChannelVisiting {
    void visitor(ChannelVisitor visitor);
}
