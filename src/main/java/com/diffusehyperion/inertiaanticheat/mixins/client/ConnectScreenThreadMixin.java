package com.diffusehyperion.inertiaanticheat.mixins.client;

import com.diffusehyperion.inertiaanticheat.interfaces.UpgradedClientLoginNetworkHandler;
import com.diffusehyperion.inertiaanticheat.interfaces.UpgradedConnectScreen;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.client.network.CookieStorage;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

// targets anonymous thread class in ConnectScreen.connect(client, address, ...)
@Mixin(targets = "net.minecraft.client.gui.screen.multiplayer.ConnectScreen$1")
public class ConnectScreenThreadMixin {
    @Shadow
    @Final
    MinecraftClient field_33738;

    @Shadow
    @Final
    ServerInfo field_40415;

    @Shadow
    @Final
    CookieStorage field_48396;

    @Shadow
    @Final
    ConnectScreen field_2416;

    @Inject(
            method = "run",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/ClientConnection;connect(Ljava/lang/String;ILnet/minecraft/network/state/NetworkState;Lnet/minecraft/network/state/NetworkState;Lnet/minecraft/network/listener/ClientPacketListener;Z)V"
            )
    )
    private void createUpgradedLoginNetworkHandler(
            CallbackInfo ci,
            @Share("loginNetworkHandler") LocalRef<ClientLoginNetworkHandler> loginNetworkHandlerLocalRef
    ) {
        ConnectScreenAccessor accessor = (ConnectScreenAccessor) field_2416;
        ClientLoginNetworkHandler handler = new ClientLoginNetworkHandler(
                accessor.getConnection(),
                field_33738,
                field_40415,
                accessor.getParent(),
                false,
                null,
                accessor::invokeSetStatus,
                field_48396
        );

        UpgradedClientLoginNetworkHandler upgradedHandler = (UpgradedClientLoginNetworkHandler) handler;
        UpgradedConnectScreen upgradedScreen = (UpgradedConnectScreen) field_2416;

        upgradedHandler.inertiaAntiCheat$setSecondaryStatusConsumer(upgradedScreen::inertiaAntiCheat$setSecondaryStatus);

        loginNetworkHandlerLocalRef.set(handler);
    }

    // ModifyArg had weird generic issues, this will work
    @ModifyArgs(
            method = "run",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/ClientConnection;connect(Ljava/lang/String;ILnet/minecraft/network/state/NetworkState;Lnet/minecraft/network/state/NetworkState;Lnet/minecraft/network/listener/ClientPacketListener;Z)V"
            )
    )
    private void replaceLoginNetworkHandler(Args args, @Share("loginNetworkHandler") LocalRef<ClientLoginNetworkHandler> loginNetworkHandlerLocalRef) {
        args.set(4, loginNetworkHandlerLocalRef.get());
    }
}
