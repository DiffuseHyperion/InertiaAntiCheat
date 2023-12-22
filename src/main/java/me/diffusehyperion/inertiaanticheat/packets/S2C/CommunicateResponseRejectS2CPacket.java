package me.diffusehyperion.inertiaanticheat.packets.S2C;

import me.diffusehyperion.inertiaanticheat.packets.ClientUpgradedQueryPacketListener;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;

public class CommunicateResponseRejectS2CPacket implements Packet<ClientUpgradedQueryPacketListener> {
    public CommunicateResponseRejectS2CPacket() {
    }

    public CommunicateResponseRejectS2CPacket(PacketByteBuf packetByteBuf) {
    }

    @Override
    public void write(PacketByteBuf buf) {
    }

    @Override
    public void apply(ClientUpgradedQueryPacketListener listener) {
        listener.onCommunicateRejectResponse(this);
    }
}
