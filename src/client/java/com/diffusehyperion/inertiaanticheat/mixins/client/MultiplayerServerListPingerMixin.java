package com.diffusehyperion.inertiaanticheat.mixins.client;

import com.diffusehyperion.inertiaanticheat.client.interfaces.UpgradedClientCollection;
import com.diffusehyperion.inertiaanticheat.client.networking.packets.UpgradedClientQueryNetworkHandler;
import com.diffusehyperion.inertiaanticheat.common.interfaces.UpgradedServerInfo;
import com.diffusehyperion.inertiaanticheat.common.networking.packets.UpgradedClientQueryPacketListener;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkingBackend;
import net.minecraft.network.listener.ClientQueryPacketListener;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;

@Mixin(MultiplayerServerListPinger.class)
public abstract class MultiplayerServerListPingerMixin {
    @Shadow
    void showError(Text error, ServerInfo info) {}
    @Shadow
    void ping(InetSocketAddress socketAddress, ServerAddress address, ServerInfo serverInfo, NetworkingBackend backend) {}

    @Inject(method = "add",
            at = @At(value = "HEAD"))
    private void setUpgradedServerPingRefs(ServerInfo entry, Runnable saver, Runnable pingCallback, NetworkingBackend backend, CallbackInfo ci,
                                           @Share("serverInfo") LocalRef<ServerInfo> serverDataLocalRef,
                                           @Share("saver") LocalRef<Runnable> saverLocalRef,
                                           @Share("pingCallback") LocalRef<Runnable> pingCallbackLocalRef,
                                           @Share("backend") LocalRef<NetworkingBackend> backendLocalRef) {
        serverDataLocalRef.set(entry);
        saverLocalRef.set(saver);
        pingCallbackLocalRef.set(pingCallback);
        backendLocalRef.set(backend);
    }

    @Redirect(method = "add",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;connect(Ljava/lang/String;ILnet/minecraft/network/listener/ClientQueryPacketListener;)V"))
    private void upgradeServerPing(
            ClientConnection connection, String host, int port, ClientQueryPacketListener clientQueryPacketListener,
            @Share("serverInfo") LocalRef<ServerInfo> serverDataLocalRef,
            @Share("saver") LocalRef<Runnable> runnableLocalRef,
            @Share("pingCallback") LocalRef<Runnable> pingCallbackLocalRef,
            @Share("backend") LocalRef<NetworkingBackend> backendLocalRef,
            @Local InetSocketAddress inetSocketAddress,
            @Local ServerAddress serverAddress) {
        ServerInfo serverInfo = serverDataLocalRef.get();
        Runnable saver = runnableLocalRef.get();
        Runnable pingCallback = pingCallbackLocalRef.get();
        NetworkingBackend backend = backendLocalRef.get();

        UpgradedClientQueryPacketListener listener =
                new UpgradedClientQueryNetworkHandler(serverInfo, saver, pingCallback, backend,
                        connection, inetSocketAddress, serverAddress,
                this::showError,
                this::ping);

        ((UpgradedServerInfo) serverInfo).inertiaAntiCheat$setInertiaInstalled(null);
        ((UpgradedServerInfo) serverInfo).inertiaAntiCheat$setAnticheatDetails(null);
        ((UpgradedClientCollection) connection).inertiaAntiCheat$connect(host, port, listener);
    }
}