package me.diffusehyperion.inertiaanticheat.mixins.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import me.diffusehyperion.inertiaanticheat.interfaces.ClientConnectionMixinInterface;
import me.diffusehyperion.inertiaanticheat.interfaces.ServerInfoInterface;
import me.diffusehyperion.inertiaanticheat.packets.UpgradedClientQueryPacketListener;
import me.diffusehyperion.inertiaanticheat.packets.UpgradedClientQueryNetworkHandler;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientQueryPacketListener;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;

@Debug(export = true)
@Mixin(MultiplayerServerListPinger.class)
public abstract class MultiplayerServerListPingerMixin {
    @Shadow
    void showError(Text error, ServerInfo info) {}
    @Shadow
    void ping(InetSocketAddress socketAddress, final ServerAddress address, final ServerInfo serverInfo) {}

    @Inject(method = "add",
            at = @At(value = "HEAD"))
    private void pingServer(ServerInfo entry, Runnable saver, Runnable pingCallback, CallbackInfo ci,
                            @Share("serverInfo") LocalRef<ServerInfo> serverDataLocalRef,
                            @Share("saver") LocalRef<Runnable> saverLocalRef,
                            @Share("pingCallback") LocalRef<Runnable> pingCallbackLocalRef) {
        serverDataLocalRef.set(entry);
        saverLocalRef.set(saver);
        pingCallbackLocalRef.set(pingCallback);
    }

    @Redirect(method = "add",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;connect(Ljava/lang/String;ILnet/minecraft/network/listener/ClientQueryPacketListener;)V"))
    private void initiateServerboundUpgradedStatusConnection(
            ClientConnection connection, String host, int port, ClientQueryPacketListener clientQueryPacketListener,
            @Share("serverInfo") LocalRef<ServerInfo> serverDataLocalRef,
            @Share("saver") LocalRef<Runnable> runnableLocalRef,
            @Share("pingCallback") LocalRef<Runnable> pingCallbackLocalRef,
            @Local InetSocketAddress inetSocketAddress,
            @Local ServerAddress serverAddress) {

        ServerInfo serverInfo = serverDataLocalRef.get();
        Runnable saver = runnableLocalRef.get();
        Runnable pingCallback = pingCallbackLocalRef.get();

        UpgradedClientQueryPacketListener listener =
                new UpgradedClientQueryNetworkHandler(serverInfo, saver, pingCallback,
                        connection, inetSocketAddress, serverAddress,
                this::showError,
                this::ping);

        ((ServerInfoInterface) serverInfo).inertiaAntiCheat$setInertiaInstalled(null);
        ((ServerInfoInterface) serverInfo).inertiaAntiCheat$setAnticheatDetails(null);
        ((ClientConnectionMixinInterface) connection).inertiaAntiCheat$connect(host, port, listener);
    }
}