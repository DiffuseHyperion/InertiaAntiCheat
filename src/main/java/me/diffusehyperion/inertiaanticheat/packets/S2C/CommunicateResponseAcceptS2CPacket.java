package me.diffusehyperion.inertiaanticheat.packets.S2C;

import me.diffusehyperion.inertiaanticheat.packets.ClientUpgradedQueryPacketListener;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;

import java.util.UUID;

public class CommunicateResponseAcceptS2CPacket implements Packet<ClientUpgradedQueryPacketListener> {
    private final UUID key;

    public CommunicateResponseAcceptS2CPacket(UUID key) {
        this.key = key;
    }

    public CommunicateResponseAcceptS2CPacket(PacketByteBuf packetByteBuf) {
        this.key = UUID.fromString(packetByteBuf.readString());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeString(this.key.toString());
    }

    @Override
    public void apply(ClientUpgradedQueryPacketListener listener) {
        listener.onCommunicateAcceptResponse(this);
    }

    public UUID getKey() {
        return this.key;
    }
}

