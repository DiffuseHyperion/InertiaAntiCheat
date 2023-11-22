package me.diffusehyperion.inertiaanticheat.packets.S2C;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.packets.ClientUpgradedQueryPacketListener;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;

import java.security.PublicKey;

public class ContactResponseEncryptedS2CPacket implements Packet<ClientUpgradedQueryPacketListener> {
    private final PublicKey publicKey;
    public ContactResponseEncryptedS2CPacket(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public ContactResponseEncryptedS2CPacket(PacketByteBuf packetByteBuf) {
        this.publicKey = InertiaAntiCheat.retrievePublicKey(packetByteBuf);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBytes(publicKey.getEncoded());
    }

    @Override
    public void apply(ClientUpgradedQueryPacketListener listener) {
        listener.onContactEncryptedResponse(this);
    }
}
