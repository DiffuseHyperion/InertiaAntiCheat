package me.diffusehyperion.inertiaanticheat.packets;

import me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

public class UpgradedServerQueryNetworkHandler implements ServerUpgradedQueryPacketListener {

    @Override
    public void onUpgradedRequest(UpgradedQueryRequestC2SPacket var1) {
        this.connection.send(new UpgradedQueryResponseS2CPacket());

        InertiaAntiCheatServer.serverScheduler.cancelTask(disconnectRunnable);
        disconnectRunnable.run();
    }

    private static final Text REQUEST_HANDLED = Text.translatable("multiplayer.status.request_handled");
    private final ServerMetadata metadata;
    private final ClientConnection connection;
    private boolean responseSent;
    private long startTime;
    private final Runnable disconnectRunnable = new Runnable() {
        @Override
        public void run() {
            connection.disconnect(REQUEST_HANDLED);
        }
    };

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
