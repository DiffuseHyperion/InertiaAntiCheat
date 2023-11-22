package me.diffusehyperion.inertiaanticheat.mixins.server;

import me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;
import net.minecraft.server.ServerNetworkIo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerNetworkIo.class)
public abstract class ServerNetworkIoMixin {
    @Inject(method = "tick",
            at = @At(value = "HEAD"))
    private void tick(CallbackInfo ci) {
        InertiaAntiCheatServer.serverScheduler.tick();
    }
}
