package com.github.phantazmnetwork.zombies.mapeditor.client.packet;

import com.github.phantazmnetwork.messaging.serialization.DataReader;
import net.minecraft.network.PacketByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A {@link DataReader} that reads from a {@link PacketByteBuf}.
 */
public class PacketByteBufDataReader implements DataReader {

    private final PacketByteBuf packetByteBuf;

    /**
     * Creates a {@link PacketByteBufDataReader}.
     *
     * @param packetByteBuf The {@link PacketByteBuf} to read from
     */
    public PacketByteBufDataReader(@NotNull PacketByteBuf packetByteBuf) {
        this.packetByteBuf = Objects.requireNonNull(packetByteBuf, "packetByteBuf");
    }

    @Override
    public byte readByte() {
        return packetByteBuf.readByte();
    }

    @Override
    public int readInt() {
        return packetByteBuf.readInt();
    }
}
