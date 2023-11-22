package me.diffusehyperion.inertiaanticheat.packets.S2C;

import me.diffusehyperion.inertiaanticheat.packets.ClientUpgradedQueryPacketListener;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;

public class CommunicateResponseS2CPacket implements Packet<ClientUpgradedQueryPacketListener> {
    private final boolean accepted;

    public CommunicateResponseS2CPacket(boolean accepted) {
        this.accepted = accepted;
    }

    public CommunicateResponseS2CPacket(PacketByteBuf packetByteBuf) {
        this.accepted = packetByteBuf.readBoolean();
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBoolean(this.accepted);
    }

    @Override
    public void apply(ClientUpgradedQueryPacketListener listener) {
        listener.onCommunicateResponse(this);
    }

    public boolean isAccepted() {
        return this.accepted;
    }
}

