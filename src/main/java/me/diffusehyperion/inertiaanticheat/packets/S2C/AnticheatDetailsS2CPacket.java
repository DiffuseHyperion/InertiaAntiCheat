package me.diffusehyperion.inertiaanticheat.packets.S2C;

import me.diffusehyperion.inertiaanticheat.packets.ClientUpgradedQueryPacketListener;
import me.diffusehyperion.inertiaanticheat.util.AnticheatDetails;
import me.diffusehyperion.inertiaanticheat.util.GroupAnticheatDetails;
import me.diffusehyperion.inertiaanticheat.util.IndividualAnticheatDetails;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;

import java.util.ArrayList;
import java.util.Arrays;

public class AnticheatDetailsS2CPacket implements Packet<ClientUpgradedQueryPacketListener> {
    private final AnticheatDetails details;

    public AnticheatDetailsS2CPacket(AnticheatDetails details) {
        this.details = details;
    }

    public AnticheatDetailsS2CPacket(PacketByteBuf packetByteBuf) {
        int ordinal = packetByteBuf.readInt();
        if (ordinal == 0) {
            this.details = new IndividualAnticheatDetails(
                    packetByteBuf.readBoolean(),
                    new ArrayList<>(Arrays.asList(packetByteBuf.readString().split(","))),
                    new ArrayList<>(Arrays.asList(packetByteBuf.readString().split(","))));
        } else if (ordinal == 1) {
            this.details = new GroupAnticheatDetails(
                    packetByteBuf.readBoolean(),
                    new ArrayList<>(Arrays.asList(packetByteBuf.readString().split(",")))
            );
        } else {
            throw new RuntimeException("Unknown ordinal given");
        }
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(this.details.getCheckMethod().ordinal());
        if (this.details instanceof IndividualAnticheatDetails individualDetails) {
            buf.writeString(String.join(",", individualDetails.getBlacklistedMods()));
            buf.writeString(String.join(",", individualDetails.getWhitelistedMods()));
        } else if (this.details instanceof GroupAnticheatDetails groupDetails) {
            buf.writeString(String.join(",", groupDetails.getModpackDetails()));
        }
    }

    @Override
    public void apply(ClientUpgradedQueryPacketListener listener) {
        listener.onReceiveAnticheatDetails(this);
    }

    public AnticheatDetails getDetails() {
        return this.details;
    }
}
