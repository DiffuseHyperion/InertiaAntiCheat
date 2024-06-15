package me.diffusehyperion.inertiaanticheat.interfaces;

import me.diffusehyperion.inertiaanticheat.packets.UpgradedClientQueryPacketListener;

public interface ClientConnectionMixinInterface {

    void inertiaAntiCheat$connect(String string, int i, UpgradedClientQueryPacketListener clientUpgradedStatusPacketListener);
}
