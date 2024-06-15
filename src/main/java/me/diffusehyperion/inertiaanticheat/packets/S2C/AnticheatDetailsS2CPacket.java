package me.diffusehyperion.inertiaanticheat.packets.S2C;

import me.diffusehyperion.inertiaanticheat.packets.AnticheatPackets;
import me.diffusehyperion.inertiaanticheat.packets.UpgradedClientQueryPacketListener;
import me.diffusehyperion.inertiaanticheat.util.AnticheatDetails;
import me.diffusehyperion.inertiaanticheat.util.GroupAnticheatDetails;
import me.diffusehyperion.inertiaanticheat.util.IndividualAnticheatDetails;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;

import java.util.ArrayList;
import java.util.Arrays;

public record AnticheatDetailsS2CPacket(AnticheatDetails details) implements Packet<UpgradedClientQueryPacketListener> {
    public static final PacketCodec<PacketByteBuf, AnticheatDetailsS2CPacket> CODEC = Packet.createCodec(AnticheatDetailsS2CPacket::write, AnticheatDetailsS2CPacket::new);

    private AnticheatDetailsS2CPacket(PacketByteBuf packetByteBuf) {
        this(bufToDetails(packetByteBuf));
    }

    private static AnticheatDetails bufToDetails(PacketByteBuf buf) {
        int ordinal = buf.readInt();
        if (ordinal == 0) {
            return new IndividualAnticheatDetails(
                    buf.readBoolean(),
                    new ArrayList<>(Arrays.asList(buf.readString().split(","))),
                    new ArrayList<>(Arrays.asList(buf.readString().split(","))));
        } else if (ordinal == 1) {
            return new GroupAnticheatDetails(
                    buf.readBoolean(),
                    new ArrayList<>(Arrays.asList(buf.readString().split(",")))
            );
        } else {
            throw new RuntimeException("Unknown ordinal given");
        }
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(this.details.getCheckMethod().ordinal());
        if (this.details instanceof IndividualAnticheatDetails individualDetails) {
            buf.writeBoolean(individualDetails.showInstalled());
            buf.writeString(String.join(",", individualDetails.getBlacklistedMods()));
            buf.writeString(String.join(",", individualDetails.getWhitelistedMods()));
        } else if (this.details instanceof GroupAnticheatDetails groupDetails) {
            buf.writeBoolean(groupDetails.showInstalled());
            buf.writeString(String.join(",", groupDetails.getModpackDetails()));
        }
    }

    public void apply(UpgradedClientQueryPacketListener listener) {
        listener.onReceiveAnticheatDetails(this);
    }

    @Override
    public PacketType<? extends Packet<UpgradedClientQueryPacketListener>> getPacketId() {
        return AnticheatPackets.DETAILS_RESPONSE;
    }
}
