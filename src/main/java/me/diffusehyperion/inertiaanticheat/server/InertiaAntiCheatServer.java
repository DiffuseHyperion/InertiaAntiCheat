package me.diffusehyperion.inertiaanticheat.server;

import com.moandjiezana.toml.Toml;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.util.HashAlgorithm;
import me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import me.diffusehyperion.inertiaanticheat.util.ModlistCheckMethod;
import me.diffusehyperion.inertiaanticheat.util.Scheduler;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;

public class InertiaAntiCheatServer implements DedicatedServerModInitializer {

    public static Toml serverConfig;
    public static ModlistCheckMethod modlistCheckMethod;
    public static HashAlgorithm hashAlgorithm;

    public static final Scheduler serverScheduler = new Scheduler();

    //TODO: Add inertiaanticheat.bypass logic
    @Override
    public void onInitializeServer() {
        InertiaAntiCheatServer.serverConfig = InertiaAntiCheat.initializeConfig("/config/server/InertiaAntiCheat.toml", InertiaAntiCheatConstants.CURRENT_SERVER_CONFIG_VERSION);

        switch (InertiaAntiCheatServer.serverConfig.getString("mods.method").toLowerCase()) {
            case "individual" -> InertiaAntiCheatServer.modlistCheckMethod = ModlistCheckMethod.INDIVIDUAL;
            case "group" -> InertiaAntiCheatServer.modlistCheckMethod = ModlistCheckMethod.GROUP;
            default -> {
                InertiaAntiCheat.error("There was an error in your config! Invalid method specified under \"mods.method\"! ");
                InertiaAntiCheat.error("Defaulting to individual method for now.");
                InertiaAntiCheatServer.modlistCheckMethod = ModlistCheckMethod.INDIVIDUAL;
            }
        }

        switch (InertiaAntiCheatServer.serverConfig.getString("mods.algorithm").toLowerCase()) {
            case "md5" -> InertiaAntiCheatServer.hashAlgorithm = HashAlgorithm.MD5;
            case "sha1" -> InertiaAntiCheatServer.hashAlgorithm = HashAlgorithm.SHA1;
            case "sha256" -> InertiaAntiCheatServer.hashAlgorithm = HashAlgorithm.SHA256;
            default -> {
                InertiaAntiCheat.error("There was an error in your config! Invalid algorithm specified under \"mods.algorithm\"! ");
                InertiaAntiCheat.error("Defaulting to MD5 algorithm for now.");
                InertiaAntiCheatServer.hashAlgorithm = HashAlgorithm.MD5;
            }
        }
        ServerLoginHandler.registerServerKeyHandler();
        ServerPlayConnectionEvents.JOIN.register(this::onPlayerJoin);
        ServerLoginConnectionEvents.QUERY_START.register(ServerLoginHandler::sendKeyRequest);
    }

    private void onPlayerJoin(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender packetSender, MinecraftServer minecraftServer) {
        InertiaAntiCheat.info("Player joining from address: " + serverPlayNetworkHandler.getConnectionAddress());
    }
}
