package me.diffusehyperion.inertiaanticheat.packets;

import me.diffusehyperion.inertiaanticheat.packets.S2C.AnticheatDetailsS2CPacket;
import me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;
import me.diffusehyperion.inertiaanticheat.util.GroupAnticheatDetails;
import me.diffusehyperion.inertiaanticheat.util.IndividualAnticheatDetails;
import me.diffusehyperion.inertiaanticheat.util.ModlistCheckMethod;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;

public class  UpgradedServerQueryNetworkHandler implements ServerUpgradedQueryPacketListener {
    /* ---------- vanilla fields ----------*/

    private static final Text REQUEST_HANDLED = Text.translatable("multiplayer.status.request_handled");
    private final ServerMetadata metadata;
    private final ClientConnection connection;
    private boolean responseSent;

    public UpgradedServerQueryNetworkHandler(ServerMetadata metadata, ClientConnection connection) {
        /* ---------- vanilla fields ----------*/

        this.metadata = metadata;
        this.connection = connection;
    }

    /* ---------- (Mostly) vanilla stuff below ----------*/

    @Override
    public void onDisconnected(Text reason) {
    }

    @Override
    public boolean isConnectionOpen() {
        return this.connection.isOpen();
    }

    @Override
    public void onRequest(QueryRequestC2SPacket packet) {
        if (this.responseSent) {
            this.connection.disconnect(REQUEST_HANDLED);
            return;
        }
        this.responseSent = true;
        this.connection.send(new QueryResponseS2CPacket(this.metadata));
    }

    @Override
    public void onQueryPing(QueryPingC2SPacket packet) {
        if (InertiaAntiCheatServer.modlistCheckMethod == ModlistCheckMethod.INDIVIDUAL) {
            IndividualAnticheatDetails details =
                    new IndividualAnticheatDetails(
                            InertiaAntiCheatServer.serverConfig.getList("mods.individual.friendlyBlacklist"),
                            InertiaAntiCheatServer.serverConfig.getList("mods.individual.friendlyWhitelist"));
            this.connection.send(new AnticheatDetailsS2CPacket(details));
        } else {
            GroupAnticheatDetails details =
                    new GroupAnticheatDetails(
                            InertiaAntiCheatServer.serverConfig.getList("mods.group.friendlyChecksum"));
            this.connection.send(new AnticheatDetailsS2CPacket(details));
        }

        this.connection.send(new PingResultS2CPacket(packet.getStartTime()));
        this.connection.disconnect(REQUEST_HANDLED);
    }
}
