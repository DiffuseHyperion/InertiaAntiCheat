package com.diffusehyperion.inertiaanticheat.common.interfaces;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.ClientConnection;

public interface UpgradedServerLoginNetworkHandler {
    ClientConnection inertiaAntiCheat$getConnection();
    GameProfile inertiaAntiCheat$getGameProfile();
}
