package me.diffusehyperion.inertiaanticheat.packets.S2C;

import me.diffusehyperion.inertiaanticheat.packets.ClientUpgradedQueryPacketListener;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;

public class CommunicateResponseS2CPacket implements Packet<ClientUpgradedQueryPacketListener> {
    private final boolean accepted;
    private final byte[] encryptedKey;

    public CommunicateResponseS2CPacket(byte[] encryptedKey) {
        this.accepted = true;
        this.encryptedKey = encryptedKey;
    }

    public CommunicateResponseS2CPacket(PacketByteBuf packetByteBuf) {
        this.accepted = packetByteBuf.readBoolean();

        if (!this.accepted) {
            this.encryptedKey = null;
        } else {
            byte[] enctyptedKey = new byte[packetByteBuf.readableBytes()];
            packetByteBuf.readBytes(enctyptedKey);
            this.encryptedKey = enctyptedKey;
        }
    }

    public CommunicateResponseS2CPacket() {
        this.accepted = false;
        this.encryptedKey = null;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBoolean(this.accepted);
        if (this.accepted) {
            buf.writeBytes(this.encryptedKey);
        }
    }

    @Override
    public void apply(ClientUpgradedQueryPacketListener listener) {
        listener.onCommunicateResponse(this);
    }

    public byte[] getEncryptedKey() {
        return this.encryptedKey;
    }

    public boolean isAccepted() {
        return this.accepted;
    }
}

