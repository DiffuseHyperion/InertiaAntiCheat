package me.diffusehyperion.inertiaanticheat.packets;

import com.mojang.authlib.GameProfile;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.client.InertiaAntiCheatClient;
import me.diffusehyperion.inertiaanticheat.interfaces.ServerInfoInterface;
import me.diffusehyperion.inertiaanticheat.packets.S2C.AnticheatDetailsS2CPacket;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.apache.logging.log4j.util.TriConsumer;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

public class UpgradedClientQueryNetworkHandler implements ClientUpgradedQueryPacketListener {
    /* ---------- vanilla fields ----------*/

    private final ServerInfo serverInfo;
    private final Runnable runnable;
    private final ClientConnection connection;

    private final InetSocketAddress inetSocketAddress;
    private final ServerAddress serverAddress;

    private final BiConsumer<Text, ServerInfo> showErrorMethod;
    private final TriConsumer<InetSocketAddress, ServerAddress, ServerInfo> pingMethod;

    private boolean sentQuery;
    private boolean received;
    private long startTime;

    public UpgradedClientQueryNetworkHandler(ServerInfo serverInfo, Runnable runnable, ClientConnection connection,
                                             InetSocketAddress inetSocketAddress, ServerAddress serverAddress,
                                             BiConsumer<Text, ServerInfo> showErrorMethod,
                                             TriConsumer<InetSocketAddress, ServerAddress, ServerInfo> pingMethod) {
        /* ---------- vanilla fields ----------*/

        this.serverInfo = serverInfo;
        this.runnable = runnable;
        this.connection = connection;

        this.inetSocketAddress = inetSocketAddress;
        this.serverAddress = serverAddress;

        this.showErrorMethod = showErrorMethod;
        this.pingMethod = pingMethod;
    }


    @Override
    public void onReceiveAnticheatDetails(AnticheatDetailsS2CPacket var1) {
        ((ServerInfoInterface) serverInfo).inertiaAntiCheat$setInertiaInstalled(true);
        ((ServerInfoInterface) serverInfo).inertiaAntiCheat$setAnticheatDetails(var1.details());
    }


    /* ---------- (Mostly) vanilla stuff below ----------*/

    @Override
    public void onResponse(QueryResponseS2CPacket packet) {
        if (this.received) {
            connection.disconnect(Text.translatable("multiplayer.status.unrequested"));
            return;
        }
        this.received = true;
        ServerMetadata serverMetadata = packet.metadata();
        serverInfo.label = serverMetadata.description();
        serverMetadata.version().ifPresentOrElse(version -> {
            serverInfo.version = Text.literal(version.gameVersion());
            serverInfo.protocolVersion = version.protocolVersion();
        }, () -> {
            serverInfo.version = Text.translatable("multiplayer.status.old");
            serverInfo.protocolVersion = 0;
        });
        serverMetadata.players().ifPresentOrElse(players -> {
            serverInfo.playerCountLabel = MultiplayerServerListPinger.createPlayerCountText(players.online(), players.max());
            serverInfo.players = players;
            if (!players.sample().isEmpty()) {
                ArrayList<Text> list = new ArrayList<>(players.sample().size());
                for (GameProfile gameProfile : players.sample()) {
                    list.add(Text.literal(gameProfile.getName()));
                }
                if (players.sample().size() < players.online()) {
                    list.add(Text.translatable("multiplayer.status.and_more", players.online() - players.sample().size()));
                }
                serverInfo.playerListSummary = list;
            } else {
                serverInfo.playerListSummary = List.of();
            }
        }, () -> serverInfo.playerCountLabel = Text.translatable("multiplayer.status.unknown").formatted(Formatting.DARK_GRAY));
        serverMetadata.favicon().ifPresent(favicon -> {
            if (!Arrays.equals(favicon.iconBytes(), serverInfo.getFavicon())) {
                serverInfo.setFavicon(ServerInfo.validateFavicon(favicon.iconBytes()));
                runnable.run();
            }
        });
        this.startTime = Util.getMeasuringTimeMs();
        this.connection.send(new QueryPingC2SPacket(this.startTime));
        this.sentQuery = true;
    }

    @Override
    public void onPingResult(PingResultS2CPacket packet) {
        long l = this.startTime;
        long m = Util.getMeasuringTimeMs();
        serverInfo.ping = m - l;
        this.connection.disconnect(Text.translatable("multiplayer.status.finished"));
    }

    @Override
    public void onDisconnected(DisconnectionInfo info) {
        if (!this.sentQuery) {
            showErrorMethod.accept(info.reason(), serverInfo);
            pingMethod.accept(inetSocketAddress, serverAddress, serverInfo);
        }
    }

    @Override
    public boolean isConnectionOpen() {
        return this.connection.isOpen();
    }
}
