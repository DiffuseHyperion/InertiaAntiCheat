package me.diffusehyperion.inertiaanticheat.packets;

import net.minecraft.network.listener.ServerQueryPacketListener;

public interface ServerUpgradedQueryPacketListener extends ServerQueryPacketListener {
    void onUpgradedRequest(UpgradedQueryRequestC2SPacket var1);
}
