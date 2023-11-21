package me.diffusehyperion.inertiaanticheat.packets;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;

public class UpgradedQueryRequestC2SPacket implements Packet<ServerUpgradedQueryPacketListener> {
    public UpgradedQueryRequestC2SPacket() {
    }

    public UpgradedQueryRequestC2SPacket(PacketByteBuf packetByteBuf) {
    }
    @Override
    public void write(PacketByteBuf buf) {

    }

    @Override
    public void apply(ServerUpgradedQueryPacketListener listener) {
        listener.onUpgradedRequest(this);
    }
}
