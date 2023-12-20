package me.diffusehyperion.inertiaanticheat.packets;

import me.diffusehyperion.inertiaanticheat.packets.C2S.CommunicateRequestEncryptedC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.C2S.CommunicateRequestUnencryptedC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.C2S.ContactRequestC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.S2C.ContactResponseEncryptedS2CPacket;
import me.diffusehyperion.inertiaanticheat.packets.S2C.ContactResponseRejectS2CPacket;
import me.diffusehyperion.inertiaanticheat.packets.S2C.ContactResponseUnencryptedS2CPacket;
import me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.Objects;

import static me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.serverE2EEKeyPair;

public class UpgradedServerQueryNetworkHandler implements ServerUpgradedQueryPacketListener {
    private long startTime;
    private final Runnable disconnectRunnable = new Runnable() {
        @Override
        public void run() {
            connection.disconnect(REQUEST_HANDLED);
        }
    };

    @Override
    public void onContactRequest(ContactRequestC2SPacket var1) {
        InertiaAntiCheatServer.serverScheduler.cancelTask(disconnectRunnable);

        boolean clientE2EESupport = var1.getE2EESupport();
        boolean serverE2EESupport = Objects.nonNull(serverE2EEKeyPair);

        if (!serverE2EESupport) {
            connection.send(new ContactResponseUnencryptedS2CPacket());
        } else if (!clientE2EESupport) {
            connection.send(new ContactResponseRejectS2CPacket());
        } else {
            connection.send(new ContactResponseEncryptedS2CPacket(serverE2EEKeyPair.getPublic()));
        }
    }

    @Override
    public void onCommunicateUnencryptedRequest(CommunicateRequestUnencryptedC2SPacket var1) {
        disconnectRunnable.run();
    }

    @Override
    public void onCommunicateEncryptedRequest(CommunicateRequestEncryptedC2SPacket var1) {
        disconnectRunnable.run();
    }

    /* ---------- (Mostly) vanilla stuff below ----------*/

    private static final Text REQUEST_HANDLED = Text.translatable("multiplayer.status.request_handled");
    private final ServerMetadata metadata;
    private final ClientConnection connection;
    private boolean responseSent;

    public UpgradedServerQueryNetworkHandler(ServerMetadata metadata, ClientConnection connection) {
        this.metadata = metadata;
        this.connection = connection;
    }

    @Override
    public void onDisconnected(Text reason) {
    }

    @Override
    public boolean isConnectionOpen() {
        return this.connection.isOpen();
    }

    @Override
    public void onRequest(QueryRequestC2SPacket packet) {
        this.startTime = Util.getMeasuringTimeMs();

        if (this.responseSent) {
            this.connection.disconnect(REQUEST_HANDLED);
            return;
        }
        this.responseSent = true;
        this.connection.send(new QueryResponseS2CPacket(this.metadata));
    }

    @Override
    public void onQueryPing(QueryPingC2SPacket packet) {
        this.connection.send(new PingResultS2CPacket(packet.getStartTime()));

        long timeTaken = Util.getMeasuringTimeMs() - this.startTime;
        InertiaAntiCheatServer.serverScheduler.addTask((int) ((timeTaken / 50) + 100), disconnectRunnable);
    }
}
