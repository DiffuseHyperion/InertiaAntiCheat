package com.diffusehyperion.inertiaanticheat.server.networking.packets;

import com.diffusehyperion.inertiaanticheat.common.networking.packets.S2C.AnticheatDetailsS2CPacket;
import com.diffusehyperion.inertiaanticheat.common.util.InertiaAntiCheatConstants;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.PacketType;

public class AnticheatPackets {
    public static final PacketType<AnticheatDetailsS2CPacket> DETAILS_RESPONSE = new PacketType<>(NetworkSide.CLIENTBOUND, InertiaAntiCheatConstants.ANTICHEAT_DETAILS_ID);
}
