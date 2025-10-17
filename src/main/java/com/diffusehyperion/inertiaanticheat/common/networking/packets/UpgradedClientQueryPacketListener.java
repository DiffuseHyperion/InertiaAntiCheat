package com.diffusehyperion.inertiaanticheat.common.networking.packets;

import com.diffusehyperion.inertiaanticheat.common.networking.packets.S2C.AnticheatDetailsS2CPacket;
import net.minecraft.network.listener.ClientQueryPacketListener;

public interface UpgradedClientQueryPacketListener extends ClientQueryPacketListener {
    void onReceiveAnticheatDetails(AnticheatDetailsS2CPacket var1);
}
