package me.diffusehyperion.inertiaanticheat.mixins.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.authlib.GameProfile;
import me.diffusehyperion.inertiaanticheat.interfaces.ClientConnectionMixinInterface;
import me.diffusehyperion.inertiaanticheat.interfaces.ServerInfoInterface;
import me.diffusehyperion.inertiaanticheat.packets.ClientUpgradedQueryPacketListener;
import me.diffusehyperion.inertiaanticheat.packets.UpgradedQueryRequestC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.UpgradedQueryResponseS2CPacket;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientQueryPacketListener;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Debug(export = true)
@Mixin(MultiplayerServerListPinger.class)
public class MultiplayerServerListPingerMixin {
    @Shadow
    void showError(Text error, ServerInfo info) {}
    @Shadow
    void ping(InetSocketAddress socketAddress, final ServerAddress address, final ServerInfo serverInfo) {}

    @Inject(method = "add",
            at = @At(value = "HEAD"))
    private void pingServer(ServerInfo entry, Runnable runnable, CallbackInfo ci,
                            @Share("serverData") LocalRef<ServerInfo> serverDataLocalRef,
                            @Share("runnable") LocalRef<Runnable> runnableLocalRef) {
        serverDataLocalRef.set(entry);
        runnableLocalRef.set(runnable);
    }

    @Redirect(method = "add",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;connect(Ljava/lang/String;ILnet/minecraft/network/listener/ClientQueryPacketListener;)V"))
    private void initiateServerboundUpgradedStatusConnection(
            ClientConnection connection, String host, int port, ClientQueryPacketListener clientQueryPacketListener,
            @Share("serverData") LocalRef<ServerInfo> serverDataLocalRef,
            @Share("runnable") LocalRef<Runnable> runnableLocalRef,
            @Local InetSocketAddress inetSocketAddress,
            @Local ServerAddress serverAddress) {
        ServerInfo serverData = serverDataLocalRef.get();
        Runnable runnable = runnableLocalRef.get();
        ClientUpgradedQueryPacketListener listener = new ClientUpgradedQueryPacketListener() {
            @Override
            public void onUpgradedResponse(UpgradedQueryResponseS2CPacket var1) {
                ((ServerInfoInterface) serverData).inertiaAntiCheat$setInertiaInstalled(true);
            }
            private boolean sentQuery;
            private boolean received;
            private long startTime;

            @Override
            public void onResponse(QueryResponseS2CPacket packet) {
                if (this.received) {
                    connection.disconnect(Text.translatable("multiplayer.status.unrequested"));
                    return;
                }
                this.received = true;
                ServerMetadata serverMetadata = packet.metadata();
                serverData.label = serverMetadata.description();
                serverMetadata.version().ifPresentOrElse(version -> {
                    serverData.version = Text.literal(version.gameVersion());
                    serverData.protocolVersion = version.protocolVersion();
                }, () -> {
                    serverData.version = Text.translatable("multiplayer.status.old");
                    serverData.protocolVersion = 0;
                });
                serverMetadata.players().ifPresentOrElse(players -> {
                    serverData.playerCountLabel = MultiplayerServerListPinger.createPlayerCountText(players.online(), players.max());
                    serverData.players = players;
                    if (!players.sample().isEmpty()) {
                        ArrayList<Text> list = new ArrayList<>(players.sample().size());
                        for (GameProfile gameProfile : players.sample()) {
                            list.add(Text.literal(gameProfile.getName()));
                        }
                        if (players.sample().size() < players.online()) {
                            list.add(Text.translatable("multiplayer.status.and_more", players.online() - players.sample().size()));
                        }
                        serverData.playerListSummary = list;
                    } else {
                        serverData.playerListSummary = List.of();
                    }
                }, () -> serverData.playerCountLabel = Text.translatable("multiplayer.status.unknown").formatted(Formatting.DARK_GRAY));
                serverMetadata.favicon().ifPresent(favicon -> {
                    if (!Arrays.equals(favicon.iconBytes(), serverData.getFavicon())) {
                        serverData.setFavicon(ServerInfo.validateFavicon(favicon.iconBytes()));
                        runnable.run();
                    }
                });
                this.startTime = Util.getMeasuringTimeMs();
                connection.send(new QueryPingC2SPacket(this.startTime));
                this.sentQuery = true;
            }

            @Override
            public void onPingResult(PingResultS2CPacket packet) {
                long l = this.startTime;
                long m = Util.getMeasuringTimeMs();
                serverData.ping = m - l;
                connection.disconnect(Text.translatable("multiplayer.status.finished"));
            }

            @Override
            public void onDisconnected(Text reason) {
                if (!this.sentQuery) {
                    MultiplayerServerListPingerMixin.this.showError(reason, serverData);
                    MultiplayerServerListPingerMixin.this.ping(inetSocketAddress, serverAddress, serverData);
                }
            }

            @Override
            public boolean isConnectionOpen() {
                return connection.isOpen();
            }
        };

        ((ClientConnectionMixinInterface) connection).inertiaAntiCheat$connect(host, port, listener);
        connection.send(new UpgradedQueryRequestC2SPacket());
    }
}