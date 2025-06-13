package com.diffusehyperion.inertiaanticheat.networking.packets;

import com.diffusehyperion.inertiaanticheat.networking.packets.S2C.AnticheatDetailsS2CPacket;
import net.minecraft.network.listener.ClientQueryPacketListener;

public interface UpgradedClientQueryPacketListener extends ClientQueryPacketListener {
    void onReceiveAnticheatDetails(AnticheatDetailsS2CPacket var1);
}
