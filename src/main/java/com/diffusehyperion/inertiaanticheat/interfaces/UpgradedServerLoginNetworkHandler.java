package com.diffusehyperion.inertiaanticheat.interfaces;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.ClientConnection;

public interface UpgradedServerLoginNetworkHandler {
    ClientConnection inertiaAntiCheat$getConnection();
    GameProfile inertiaAntiCheat$getGameProfile();
}
