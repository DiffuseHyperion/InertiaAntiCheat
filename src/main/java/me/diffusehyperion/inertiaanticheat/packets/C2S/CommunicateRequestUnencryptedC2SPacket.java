package me.diffusehyperion.inertiaanticheat.packets.C2S;

import me.diffusehyperion.inertiaanticheat.packets.ServerUpgradedQueryPacketListener;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;

public class CommunicateRequestUnencryptedC2SPacket implements Packet<ServerUpgradedQueryPacketListener> {
    private final String modlistHash;

    public CommunicateRequestUnencryptedC2SPacket(String modlistHash) {
        this.modlistHash = modlistHash;
    }

    public CommunicateRequestUnencryptedC2SPacket(PacketByteBuf packetByteBuf) {
        this.modlistHash = packetByteBuf.readString();
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeString(this.modlistHash);
    }

    @Override
    public void apply(ServerUpgradedQueryPacketListener listener) {
        listener.onCommunicateUnencryptedRequest(this);
    }

    public String getModlistHash() {
        return this.modlistHash;
    }
}
