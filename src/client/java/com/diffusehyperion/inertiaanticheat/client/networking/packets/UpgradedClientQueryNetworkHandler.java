package com.diffusehyperion.inertiaanticheat.client.networking.packets;

import com.diffusehyperion.inertiaanticheat.common.interfaces.UpgradedServerInfo;
import com.diffusehyperion.inertiaanticheat.common.networking.packets.S2C.AnticheatDetailsS2CPacket;
import com.diffusehyperion.inertiaanticheat.common.networking.packets.UpgradedClientQueryPacketListener;
import com.diffusehyperion.inertiaanticheat.utils.QuadConsumer;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.NetworkingBackend;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

public class UpgradedClientQueryNetworkHandler implements UpgradedClientQueryPacketListener {
    /* ---------- vanilla fields ----------*/

    private final ServerInfo entry;
    private final Runnable saver;
    private final Runnable pingCallback;
    private final NetworkingBackend backend;

    private final ClientConnection clientConnection;

    private final InetSocketAddress inetSocketAddress;
    private final ServerAddress serverAddress;

    private final BiConsumer<Text, ServerInfo> showErrorMethod;
    private final QuadConsumer<InetSocketAddress, ServerAddress, ServerInfo, NetworkingBackend> pingMethod;

    private boolean sentQuery;
    private boolean received;
    private long startTime;

    public UpgradedClientQueryNetworkHandler(ServerInfo entry, Runnable saver, Runnable pingCallback, NetworkingBackend backend,
                                             ClientConnection clientConnection,
                                             InetSocketAddress inetSocketAddress, ServerAddress serverAddress,
                                             BiConsumer<Text, ServerInfo> showErrorMethod,
                                             QuadConsumer<InetSocketAddress, ServerAddress, ServerInfo, NetworkingBackend> pingMethod) {
        /* ---------- vanilla fields ----------*/

        this.entry = entry;
        this.saver = saver;
        this.pingCallback = pingCallback;
        this.backend = backend;

        this.clientConnection = clientConnection;

        this.inetSocketAddress = inetSocketAddress;
        this.serverAddress = serverAddress;

        this.showErrorMethod = showErrorMethod;
        this.pingMethod = pingMethod;
    }

    @Override
    public void onReceiveAnticheatDetails(AnticheatDetailsS2CPacket var1) {
        ((UpgradedServerInfo) entry).inertiaAntiCheat$setInertiaInstalled(true);
        ((UpgradedServerInfo) entry).inertiaAntiCheat$setAnticheatDetails(var1.details());
    }


    /* ---------- (Mostly) vanilla stuff below ----------*/

    @Override
    public void onResponse(QueryResponseS2CPacket packet) {
        if (this.received) {
            clientConnection.disconnect(Text.translatable("multiplayer.status.unrequested"));
        } else {
            this.received = true;
            ServerMetadata serverMetadata = packet.metadata();
            entry.label = serverMetadata.description();
            serverMetadata.version().ifPresentOrElse(version -> {
                entry.version = Text.literal(version.gameVersion());
                entry.protocolVersion = version.protocolVersion();
            }, () -> {
                entry.version = Text.translatable("multiplayer.status.old");
                entry.protocolVersion = 0;
            });
            serverMetadata.players().ifPresentOrElse(players -> {
                entry.playerCountLabel = MultiplayerServerListPinger.createPlayerCountText(players.online(), players.max());
                entry.players = players;
                if (!players.sample().isEmpty()) {
                    List<Text> list = new ArrayList<>(players.sample().size());

                    for (PlayerConfigEntry playerConfigEntry : players.sample()) {
                        Text text;
                        if (playerConfigEntry.equals(MinecraftServer.ANONYMOUS_PLAYER_PROFILE)) {
                            text = Text.translatable("multiplayer.status.anonymous_player");
                        } else {
                            text = Text.literal(playerConfigEntry.name());
                        }

                        list.add(text);
                    }

                    if (players.sample().size() < players.online()) {
                        list.add(Text.translatable("multiplayer.status.and_more", players.online() - players.sample().size()));
                    }

                    entry.playerListSummary = list;
                } else {
                    entry.playerListSummary = List.of();
                }
            }, () -> entry.playerCountLabel = Text.translatable("multiplayer.status.unknown").formatted(Formatting.DARK_GRAY));
            serverMetadata.favicon().ifPresent(favicon -> {
                if (!Arrays.equals(favicon.iconBytes(), entry.getFavicon())) {
                    entry.setFavicon(ServerInfo.validateFavicon(favicon.iconBytes()));
                    saver.run();
                }
            });
            this.startTime = Util.getMeasuringTimeMs();
            clientConnection.send(new QueryPingC2SPacket(this.startTime));
            this.sentQuery = true;
        }
    }

    @Override
    public void onPingResult(PingResultS2CPacket packet) {
        long l = this.startTime;
        long m = Util.getMeasuringTimeMs();
        entry.ping = m - l;
        this.clientConnection.disconnect(Text.translatable("multiplayer.status.finished"));
        this.pingCallback.run();
    }

    @Override
    public void onDisconnected(DisconnectionInfo info) {
        if (!this.sentQuery) {
            showErrorMethod.accept(info.reason(), entry);
            pingMethod.accept(inetSocketAddress, serverAddress, entry, backend);
        }
    }

    @Override
    public boolean isConnectionOpen() {
        return this.clientConnection.isOpen();
    }
}
