package me.diffusehyperion.inertiaanticheat.packets.S2C;

import me.diffusehyperion.inertiaanticheat.packets.ClientUpgradedQueryPacketListener;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;

public class ContactResponseRejectS2CPacket implements Packet<ClientUpgradedQueryPacketListener> {
    public ContactResponseRejectS2CPacket() {
    }

    public ContactResponseRejectS2CPacket(PacketByteBuf packetByteBuf) {
    }

    @Override
    public void write(PacketByteBuf buf) {
    }

    @Override
    public void apply(ClientUpgradedQueryPacketListener listener) {
        listener.onContactReject(this);
    }
}
