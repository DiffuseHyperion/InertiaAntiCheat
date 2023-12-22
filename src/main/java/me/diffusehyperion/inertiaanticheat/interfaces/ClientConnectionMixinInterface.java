package me.diffusehyperion.inertiaanticheat.interfaces;

import me.diffusehyperion.inertiaanticheat.packets.ClientUpgradedQueryPacketListener;

public interface ClientConnectionMixinInterface {

    void inertiaAntiCheat$connect(String string, int i, ClientUpgradedQueryPacketListener clientUpgradedStatusPacketListener);
}
