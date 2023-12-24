package me.diffusehyperion.inertiaanticheat.interfaces;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.ClientConnection;

public interface ServerLoginNetworkHandlerInterface {
    ClientConnection inertiaAntiCheat$getConnection();
    GameProfile inertiaAntiCheat$getGameProfile();
}
