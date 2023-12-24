package me.diffusehyperion.inertiaanticheat.packets.S2C;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.packets.ClientUpgradedQueryPacketListener;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;

import java.security.PublicKey;

public class ContactResponseS2CPacket implements Packet<ClientUpgradedQueryPacketListener> {
    private final PublicKey serverPublicKey;

    public ContactResponseS2CPacket(PublicKey publicKey) {
        this.serverPublicKey = publicKey;
    }

    public ContactResponseS2CPacket(PacketByteBuf packetByteBuf) {
        this.serverPublicKey = InertiaAntiCheat.retrievePublicKey(packetByteBuf);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBytes(this.serverPublicKey.getEncoded());
    }

    @Override
    public void apply(ClientUpgradedQueryPacketListener listener) {
        listener.onContactResponse(this);
    }

    public PublicKey getServerPublicKey() {
        return serverPublicKey;
    }
}
