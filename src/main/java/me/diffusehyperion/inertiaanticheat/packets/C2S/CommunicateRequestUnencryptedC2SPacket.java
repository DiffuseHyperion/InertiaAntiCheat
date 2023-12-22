package me.diffusehyperion.inertiaanticheat.packets.C2S;

import me.diffusehyperion.inertiaanticheat.packets.ServerUpgradedQueryPacketListener;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;

public class CommunicateRequestUnencryptedC2SPacket implements Packet<ServerUpgradedQueryPacketListener> {
    private final byte[] serializedModlist;

    public CommunicateRequestUnencryptedC2SPacket(byte[] serializedModlist) {
        this.serializedModlist = serializedModlist;
    }

    public CommunicateRequestUnencryptedC2SPacket(PacketByteBuf packetByteBuf) {
        byte[] serializedModlist = new byte[packetByteBuf.readableBytes()];
        packetByteBuf.readBytes(serializedModlist);
        this.serializedModlist = serializedModlist;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBytes(this.serializedModlist);
    }

    @Override
    public void apply(ServerUpgradedQueryPacketListener listener) {
        listener.onCommunicateUnencryptedRequest(this);
    }

    public byte[] getSerializedModlist() {
        return this.serializedModlist;
    }
}
