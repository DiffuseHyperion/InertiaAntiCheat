package me.diffusehyperion.inertiaanticheat.mixins.server;

import com.mojang.authlib.GameProfile;
import me.diffusehyperion.inertiaanticheat.interfaces.ServerLoginNetworkHandlerInterface;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin implements ServerLoginNetworkHandlerInterface {
    @Shadow @Final
    ClientConnection connection;

    @Shadow private @Nullable GameProfile profile;

    @Override
    public ClientConnection inertiaAntiCheat$getConnection() {
        return this.connection;
    }

    @Override
    public GameProfile inertiaAntiCheat$getGameProfile() {
        return this.profile;
    }
}
