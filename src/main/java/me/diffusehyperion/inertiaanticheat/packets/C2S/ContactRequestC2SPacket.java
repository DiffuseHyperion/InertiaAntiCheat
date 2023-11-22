package me.diffusehyperion.inertiaanticheat.packets.C2S;

import me.diffusehyperion.inertiaanticheat.packets.ServerUpgradedQueryPacketListener;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;

public class ContactRequestC2SPacket implements Packet<ServerUpgradedQueryPacketListener> {
    private final boolean supportE2EE;
    public ContactRequestC2SPacket(boolean supportE2EE) {
        this.supportE2EE = supportE2EE;
    }

    public ContactRequestC2SPacket(PacketByteBuf packetByteBuf) {
        this.supportE2EE = packetByteBuf.readBoolean();
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBoolean(this.supportE2EE);
    }

    @Override
    public void apply(ServerUpgradedQueryPacketListener listener) {
        listener.onContactRequest(this);
    }

    public boolean getE2EESupport() {
        return this.supportE2EE;
    }
}
