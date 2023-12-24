package me.diffusehyperion.inertiaanticheat.packets;

import com.mojang.authlib.GameProfile;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.client.InertiaAntiCheatClient;
import me.diffusehyperion.inertiaanticheat.interfaces.ServerInfoInterface;
import me.diffusehyperion.inertiaanticheat.packets.C2S.CommunicateRequestC2SPacket;
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

    /* ---------- custom fields ----------*/

    private final Runnable disconnectRunnable;
    private final KeyPair keyPair;
    private final SecretKey secretKey;

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

        /* ---------- custom fields ----------*/

        this.disconnectRunnable = () -> {
            InertiaAntiCheat.debugInfo("Disconnected from address " + this.connection.getAddress());
            connection.disconnect(Text.translatable("multiplayer.status.finished"));
        };
        this.keyPair = InertiaAntiCheat.createRSAPair();
        this.secretKey = InertiaAntiCheat.createAESKey();
    }

    public void onContactResponse(ContactResponseS2CPacket var1) {
        InertiaAntiCheat.debugInfo("Received contact response from address " + this.connection.getAddress());
        InertiaAntiCheatClient.clientScheduler.cancelTask(this.disconnectRunnable);

        ((ServerInfoInterface) this.serverInfo).inertiaAntiCheat$setInertiaInstalled(true);

        byte[] serializedModlist = InertiaAntiCheatClient.serializeModlist();
        byte[] encryptedSerializedModlist = InertiaAntiCheat.encryptAESBytes(serializedModlist, this.secretKey);
        byte[] encryptedSecretKey = InertiaAntiCheat.encryptRSABytes(this.secretKey.getEncoded(), var1.getServerPublicKey());
        this.connection.send(new CommunicateRequestC2SPacket(encryptedSerializedModlist, encryptedSecretKey));
        InertiaAntiCheat.debugInfo("Sent communication request to address " + this.connection.getAddress());
        InertiaAntiCheat.debugLine();
    }

    @Override
    public void onCommunicateResponse(CommunicateResponseS2CPacket var1) {
        InertiaAntiCheat.debugInfo("Received communication response from address " + this.connection.getAddress());
        ((ServerInfoInterface) this.serverInfo).inertiaAntiCheat$setAllowedToJoin(var1.isAccepted());

        if (var1.isAccepted()) {
            byte[] decryptedKey = InertiaAntiCheat.decryptRSABytes(var1.getEncryptedKey(), this.keyPair.getPrivate());
            UUID key = InertiaAntiCheat.bytesToUUID(decryptedKey);

            InertiaAntiCheatClient.storedKeys.put(this.serverInfo, key);
        }

        this.disconnectRunnable.run();
        InertiaAntiCheat.debugLine();
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

        ((ServerInfoInterface) serverInfo).inertiaAntiCheat$setInertiaInstalled(null);
        ((ServerInfoInterface) serverInfo).inertiaAntiCheat$setAllowedToJoin(null);

        InertiaAntiCheat.debugLine();
        InertiaAntiCheat.debugInfo("Sending contact request to address " + connection.getAddress());
        connection.send(new ContactRequestC2SPacket(keyPair.getPublic()));
        InertiaAntiCheatClient.clientScheduler.addTask((int) (((serverInfo.ping / 2) / 50) + 100), disconnectRunnable);
        InertiaAntiCheat.debugLine();
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
