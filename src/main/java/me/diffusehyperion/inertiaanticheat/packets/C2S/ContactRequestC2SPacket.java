package me.diffusehyperion.inertiaanticheat.packets.C2S;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.packets.ServerUpgradedQueryPacketListener;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;

import java.security.PublicKey;

public class ContactRequestC2SPacket implements Packet<ServerUpgradedQueryPacketListener> {
    private final PublicKey clientPublicKey;

    public ContactRequestC2SPacket(PublicKey clientPublicKey) {
        this.clientPublicKey = clientPublicKey;
    }

    public ContactRequestC2SPacket(PacketByteBuf packetByteBuf) {
        this.clientPublicKey = InertiaAntiCheat.retrievePublicKey(packetByteBuf);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBytes(this.clientPublicKey.getEncoded());
    }

    @Override
    public void apply(ServerUpgradedQueryPacketListener listener) {
        listener.onContactRequest(this);
    }

    public PublicKey getClientPublicKey() {
        return this.clientPublicKey;
    }
}
