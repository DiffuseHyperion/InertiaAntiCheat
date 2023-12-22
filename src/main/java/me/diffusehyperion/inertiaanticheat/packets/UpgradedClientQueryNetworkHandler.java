package me.diffusehyperion.inertiaanticheat.packets;

import com.mojang.authlib.GameProfile;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.client.InertiaAntiCheatClient;
import me.diffusehyperion.inertiaanticheat.interfaces.ServerInfoInterface;
import me.diffusehyperion.inertiaanticheat.packets.C2S.CommunicateRequestEncryptedC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.C2S.CommunicateRequestUnencryptedC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.C2S.ContactRequestC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.S2C.*;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.apache.logging.log4j.util.TriConsumer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import static me.diffusehyperion.inertiaanticheat.client.InertiaAntiCheatClient.clientE2EESecretKey;

public class UpgradedClientQueryNetworkHandler implements ClientUpgradedQueryPacketListener {
    private final ServerInfo serverInfo;
    private final Runnable runnable;
    private final ClientConnection connection;

    private final InetSocketAddress inetSocketAddress;
    private final ServerAddress serverAddress;

    private final BiConsumer<Text, ServerInfo> showErrorMethod;
    private final TriConsumer<InetSocketAddress, ServerAddress, ServerInfo> pingMethod;

    private final Runnable disconnectRunnable = new Runnable() {
        @Override
        public void run() {
            InertiaAntiCheat.info("Disconnected");
            connection.disconnect(Text.translatable("multiplayer.status.finished"));
        }
    };

    public UpgradedClientQueryNetworkHandler(ServerInfo serverInfo, Runnable runnable, ClientConnection connection,
                                             InetSocketAddress inetSocketAddress, ServerAddress serverAddress,
                                             BiConsumer<Text, ServerInfo> showErrorMethod,
                                             TriConsumer<InetSocketAddress, ServerAddress, ServerInfo> pingMethod) {
        this.serverInfo = serverInfo;
        this.runnable = runnable;
        this.connection = connection;

        this.inetSocketAddress = inetSocketAddress;
        this.serverAddress = serverAddress;

        this.showErrorMethod = showErrorMethod;
        this.pingMethod = pingMethod;
    }

    private boolean sentQuery;
    private boolean received;
    private long startTime;

    @Override
    public void onContactReject(ContactResponseRejectS2CPacket var1) {
        InertiaAntiCheat.info("Finished contact, rejected");
        InertiaAntiCheatClient.clientScheduler.cancelTask(disconnectRunnable);
        disconnectRunnable.run();

        ((ServerInfoInterface) serverInfo).inertiaAntiCheat$setInertiaInstalled(true);
        ((ServerInfoInterface) serverInfo).inertiaAntiCheat$setAllowedToJoin(false);
    }

    @Override
    public void onContactUnencryptedResponse(ContactResponseUnencryptedS2CPacket var1) {
        InertiaAntiCheat.info("Finished unencrypted contact");
        InertiaAntiCheatClient.clientScheduler.cancelTask(disconnectRunnable);

        ((ServerInfoInterface) serverInfo).inertiaAntiCheat$setInertiaInstalled(true);

        connection.send(new CommunicateRequestUnencryptedC2SPacket(InertiaAntiCheatClient.serializeModlist()));
    }

    public void onContactEncryptedResponse(ContactResponseEncryptedS2CPacket var1) {
        InertiaAntiCheat.info("Finished encrypted contact");
        InertiaAntiCheatClient.clientScheduler.cancelTask(disconnectRunnable);

        ((ServerInfoInterface) serverInfo).inertiaAntiCheat$setInertiaInstalled(true);

        byte[] serializedModlist = InertiaAntiCheatClient.serializeModlist();
        byte[] encryptedSerializedModlist = InertiaAntiCheat.encryptAESBytes(serializedModlist, clientE2EESecretKey);
        byte[] encryptedSecretKey = InertiaAntiCheat.encryptRSABytes(clientE2EESecretKey.getEncoded(), var1.getPublicKey());
        connection.send(new CommunicateRequestEncryptedC2SPacket(encryptedSerializedModlist, encryptedSecretKey));
    }

    //TODO: Store keys for each server

    @Override
    public void onCommunicateAcceptResponse(CommunicateResponseAcceptS2CPacket var1) {
        InertiaAntiCheat.info("Finished communication, allowed to join");
        ((ServerInfoInterface) serverInfo).inertiaAntiCheat$setAllowedToJoin(true);
        disconnectRunnable.run();
    }

    @Override
    public void onCommunicateRejectResponse(CommunicateResponseRejectS2CPacket var1) {
        InertiaAntiCheat.info("Finished communication, not allowed to join");
        ((ServerInfoInterface) serverInfo).inertiaAntiCheat$setAllowedToJoin(false);
        disconnectRunnable.run();
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
        connection.send(new QueryPingC2SPacket(this.startTime));
        this.sentQuery = true;
    }

    @Override
    public void onPingResult(PingResultS2CPacket packet) {
        long l = this.startTime;
        long m = Util.getMeasuringTimeMs();
        serverInfo.ping = m - l;

        InertiaAntiCheat.info("Sending contact request");
        connection.send(new ContactRequestC2SPacket(Objects.nonNull(clientE2EESecretKey)));
        InertiaAntiCheatClient.clientScheduler.addTask((int) (((serverInfo.ping / 2) / 50) + 100), disconnectRunnable);
    }

    @Override
    public void onDisconnected(Text reason) {
        if (!this.sentQuery) {
            showErrorMethod.accept(reason, serverInfo);
            pingMethod.accept(inetSocketAddress, serverAddress, serverInfo);
        }
    }

    @Override
    public boolean isConnectionOpen() {
        return connection.isOpen();
    }
}
