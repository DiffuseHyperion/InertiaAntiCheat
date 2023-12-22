package me.diffusehyperion.inertiaanticheat.mixins.server;

import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {
    @Shadow public abstract void disconnect(Text reason);

    @Inject(method = "onHello",
    at = @At(value = "INVOKE_ASSIGN",
    target = "Lnet/minecraft/network/packet/c2s/login/LoginHelloC2SPacket;name()Ljava/lang/String;",
    ordinal = 1),
    cancellable = true)
    private void denyLogin(LoginHelloC2SPacket packet, CallbackInfo ci) {
        disconnect(Text.of("denied lol"));
        ci.cancel();
    }
}
