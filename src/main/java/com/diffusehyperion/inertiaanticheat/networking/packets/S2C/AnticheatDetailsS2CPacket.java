package com.diffusehyperion.inertiaanticheat.networking.packets.S2C;

import com.diffusehyperion.inertiaanticheat.networking.packets.AnticheatPackets;
import com.diffusehyperion.inertiaanticheat.networking.packets.UpgradedClientQueryPacketListener;
import com.diffusehyperion.inertiaanticheat.util.AnticheatDetails;
import com.diffusehyperion.inertiaanticheat.util.GroupAnticheatDetails;
import com.diffusehyperion.inertiaanticheat.util.IndividualAnticheatDetails;
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

    @Override
    public PacketType<? extends Packet<UpgradedClientQueryPacketListener>> getPacketType() {
        return AnticheatPackets.DETAILS_RESPONSE;
    }

    public void apply(UpgradedClientQueryPacketListener listener) {
        listener.onReceiveAnticheatDetails(this);
    }
}
