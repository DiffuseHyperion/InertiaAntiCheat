package me.diffusehyperion.inertiaanticheat.packets;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;

public class UpgradedQueryResponseS2CPacket implements Packet<ClientUpgradedQueryPacketListener> {

    public UpgradedQueryResponseS2CPacket() {
    }

    public UpgradedQueryResponseS2CPacket(PacketByteBuf packetByteBuf) {
    }
    @Override
    public void write(PacketByteBuf buf) {

    }

    @Override
    public void apply(ClientUpgradedQueryPacketListener listener) {
        listener.onUpgradedResponse(this);
    }
}
