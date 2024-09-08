package me.diffusehyperion.inertiaanticheat.networking.packets;

import me.diffusehyperion.inertiaanticheat.networking.packets.S2C.*;
import net.minecraft.network.listener.ClientQueryPacketListener;

public interface UpgradedClientQueryPacketListener extends ClientQueryPacketListener {
    void onReceiveAnticheatDetails(AnticheatDetailsS2CPacket var1);
}
