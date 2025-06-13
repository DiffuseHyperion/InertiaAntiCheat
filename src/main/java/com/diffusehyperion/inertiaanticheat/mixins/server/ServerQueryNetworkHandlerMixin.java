package com.diffusehyperion.inertiaanticheat.mixins.server;

import com.diffusehyperion.inertiaanticheat.networking.packets.S2C.AnticheatDetailsS2CPacket;
import com.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;
import com.diffusehyperion.inertiaanticheat.util.GroupAnticheatDetails;
import com.diffusehyperion.inertiaanticheat.util.IndividualAnticheatDetails;
import com.diffusehyperion.inertiaanticheat.util.ValidationMethod;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.server.network.ServerQueryNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerQueryNetworkHandler.class)
public abstract class ServerQueryNetworkHandlerMixin {
    @Shadow @Final
    private ClientConnection connection;

    @Inject(method = "onQueryPing",
    at = @At(value = "HEAD"))
    private void injectSendAnticheatDetails(QueryPingC2SPacket packet, CallbackInfo ci) {
        if (InertiaAntiCheatServer.validationMethod == ValidationMethod.INDIVIDUAL) {
            IndividualAnticheatDetails details =
                    new IndividualAnticheatDetails(
                            InertiaAntiCheatServer.serverConfig.getBoolean("motd.showInstalled"),
                            InertiaAntiCheatServer.serverConfig.getList("motd.blacklist"),
                            InertiaAntiCheatServer.serverConfig.getList("motd.whitelist"));
            this.connection.send(new AnticheatDetailsS2CPacket(details));
        } else {
            GroupAnticheatDetails details =
                    new GroupAnticheatDetails(
                            InertiaAntiCheatServer.serverConfig.getBoolean("motd.showInstalled"),
                            InertiaAntiCheatServer.serverConfig.getList("motd.hash"));
            this.connection.send(new AnticheatDetailsS2CPacket(details));
        }
    }
}
