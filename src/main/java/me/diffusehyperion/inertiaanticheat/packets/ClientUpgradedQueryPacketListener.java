package me.diffusehyperion.inertiaanticheat.packets;

import me.diffusehyperion.inertiaanticheat.packets.S2C.*;
import net.minecraft.network.listener.ClientQueryPacketListener;

public interface ClientUpgradedQueryPacketListener extends ClientQueryPacketListener {
    void onReceiveAnticheatDetails(AnticheatDetailsS2CPacket var1);
}
