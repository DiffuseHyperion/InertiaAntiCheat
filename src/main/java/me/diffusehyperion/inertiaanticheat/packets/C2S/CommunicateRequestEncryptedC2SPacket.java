package me.diffusehyperion.inertiaanticheat.packets.C2S;

import me.diffusehyperion.inertiaanticheat.packets.ServerUpgradedQueryPacketListener;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;

public class CommunicateRequestEncryptedC2SPacket implements Packet<ServerUpgradedQueryPacketListener> {
    private final byte[] encryptedAESModlistHash;
    private final byte[] encrypytedRSAAESKey;

    public CommunicateRequestEncryptedC2SPacket(byte[] encryptedAESModlistHash, byte[] encrypytedRSAAESKey) {
        this.encryptedAESModlistHash = encryptedAESModlistHash;
        this.encrypytedRSAAESKey = encrypytedRSAAESKey;
    }

    public CommunicateRequestEncryptedC2SPacket(PacketByteBuf packetByteBuf) {
        int length = packetByteBuf.readInt();

        byte[] encryptedModlistHash = new byte[length];
        packetByteBuf.readBytes(encryptedModlistHash);
        this.encryptedAESModlistHash = encryptedModlistHash;

        byte[] encryptedAESKey = new byte[packetByteBuf.readableBytes()];
        packetByteBuf.readBytes(encryptedAESKey);
        this.encrypytedRSAAESKey = encryptedAESKey;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(this.encryptedAESModlistHash.length);
        buf.writeBytes(this.encryptedAESModlistHash);
        buf.writeBytes(this.encrypytedRSAAESKey);
    }

    @Override
    public void apply(ServerUpgradedQueryPacketListener listener) {
        listener.onCommunicateEncryptedRequest(this);
    }

    public byte[] getEncryptedAESModlistHash() {
        return this.encryptedAESModlistHash;
    }

    public byte[] getEncrypytedRSAAESKey() {
        return this.encrypytedRSAAESKey;
    }
}
