package me.diffusehyperion.inertiaanticheat.packets.C2S;

import me.diffusehyperion.inertiaanticheat.packets.ServerUpgradedQueryPacketListener;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;

public class CommunicateRequestUnencryptedC2SPacket implements Packet<ServerUpgradedQueryPacketListener> {
    private final String serializedModlist;

    public CommunicateRequestUnencryptedC2SPacket(String serializedModlist) {
        this.serializedModlist = serializedModlist;
    }

    public CommunicateRequestUnencryptedC2SPacket(PacketByteBuf packetByteBuf) {
        this.serializedModlist = packetByteBuf.readString();
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeString(this.serializedModlist);
    }

    @Override
    public void apply(ServerUpgradedQueryPacketListener listener) {
        listener.onCommunicateUnencryptedRequest(this);
    }

    public String getSerializedModlist() {
        return this.serializedModlist;
    }
}
