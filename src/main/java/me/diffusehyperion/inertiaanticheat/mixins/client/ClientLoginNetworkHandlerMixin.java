package me.diffusehyperion.inertiaanticheat.mixins.client;

import me.diffusehyperion.inertiaanticheat.interfaces.ClientLoginNetworkHandlerInterface;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientLoginNetworkHandler.class)
public abstract class ClientLoginNetworkHandlerMixin implements ClientLoginNetworkHandlerInterface {
    @Shadow @Final private @Nullable ServerInfo serverInfo;

    @Override
    public ServerInfo inertiaAntiCheat$getServerInfo() {
        return this.serverInfo;
    }
}
