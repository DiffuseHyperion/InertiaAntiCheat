package me.diffusehyperion.inertiaanticheat.packets;

import net.minecraft.network.listener.ClientQueryPacketListener;

public interface ClientUpgradedQueryPacketListener extends ClientQueryPacketListener {
    void onUpgradedResponse(UpgradedQueryResponseS2CPacket var1);
}
