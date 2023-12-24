package me.diffusehyperion.inertiaanticheat.packets.C2S;

import me.diffusehyperion.inertiaanticheat.packets.ServerUpgradedQueryPacketListener;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;

public class CommunicateRequestC2SPacket implements Packet<ServerUpgradedQueryPacketListener> {
    private final byte[] encryptedAESSerializedModlist;
    private final byte[] encryptedRSAAESKey;

    public CommunicateRequestC2SPacket(byte[] encryptedAESModlistHash, byte[] encryptedRSAAESKey) {
        this.encryptedAESSerializedModlist = encryptedAESModlistHash;
        this.encryptedRSAAESKey = encryptedRSAAESKey;
    }

    public CommunicateRequestC2SPacket(PacketByteBuf packetByteBuf) {
        int length = packetByteBuf.readInt();

        byte[] encryptedAESSerializedModlist = new byte[length];
        packetByteBuf.readBytes(encryptedAESSerializedModlist);
        this.encryptedAESSerializedModlist = encryptedAESSerializedModlist;

        byte[] encrypytedRSAAESKey = new byte[packetByteBuf.readableBytes()];
        packetByteBuf.readBytes(encrypytedRSAAESKey);
        this.encryptedRSAAESKey = encrypytedRSAAESKey;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(this.encryptedAESSerializedModlist.length);
        buf.writeBytes(this.encryptedAESSerializedModlist);
        buf.writeBytes(this.encryptedRSAAESKey);
    }

    @Override
    public void apply(ServerUpgradedQueryPacketListener listener) {
        listener.onCommunicateRequest(this);
    }

    public byte[] getEncryptedAESSerializedModlist() {
        return this.encryptedAESSerializedModlist;
    }

    public byte[] getEncryptedRSAAESKey() {
        return this.encryptedRSAAESKey;
    }
}
