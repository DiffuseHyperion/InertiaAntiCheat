package me.diffusehyperion.inertiaanticheat.interfaces;

import me.diffusehyperion.inertiaanticheat.networking.packets.UpgradedClientQueryPacketListener;

public interface ClientConnectionMixinInterface {

    void inertiaAntiCheat$connect(String string, int i, UpgradedClientQueryPacketListener clientUpgradedStatusPacketListener);
}
