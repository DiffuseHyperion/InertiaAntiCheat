package me.diffusehyperion.inertiaanticheat.server;

import com.moandjiezana.toml.Toml;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants;
import me.diffusehyperion.inertiaanticheat.packets.ModListResponseC2SPacket;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants.CURRENT_CONFIG_VERSION;
import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants.LOGGER;

public class InertiaAntiCheatServer implements DedicatedServerModInitializer {

    public static HashMap<ServerPlayerEntity, Long> impendingPlayers = new HashMap<>();
    public static Toml config;
    @Override
    public void onInitializeServer() {
        initializeConfig();
        initializeListeners();
    }

    private void initializeConfig() {
        File configFile = FabricLoader.getInstance().getConfigDir().resolve("InertiaAntiCheat.toml").toFile();
        if (!configFile.exists()) {
            LOGGER.warn("No config file found! Creating a new one now...");
            try {
                Files.copy(Objects.requireNonNull(InertiaAntiCheatServer.class.getResourceAsStream("/InertiaAntiCheat.toml")), configFile.toPath());
            } catch (IOException e) {
                LOGGER.error("Couldn't create default config!", e);
                return;
            }
        }
        config = new Toml().read(configFile);
        InertiaAntiCheat.debugInfo("Config version: " + config.getLong("debug.version"));
        if (config.getLong("debug.version", 0L) != CURRENT_CONFIG_VERSION) {
            LOGGER.warn("Looks like your config file is outdated! Backing up current config, then creating an updated config.");
            LOGGER.warn("Your config file will be backed up to \"BACKUP-InertiaAntiCheat.toml\".");
            File backupFile = FabricLoader.getInstance().getConfigDir().resolve("BACKUP-InertiaAntiCheat.toml").toFile();
            try {
                Files.copy(configFile.toPath(), backupFile.toPath());
                if (!configFile.delete()) {
                    throw new IOException("Could not delete config file!");
                }
                Files.copy(Objects.requireNonNull(InertiaAntiCheatServer.class.getResourceAsStream("/InertiaAntiCheat.toml")), configFile.toPath());
            } catch (IOException e) {
                LOGGER.error("Couldn't copy existing config file into a backup config file!", e);
            }
            LOGGER.info("Done! Please readjust the configs in the new file accordingly.");
        }
    }

    private void initializeListeners() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof ServerPlayerEntity player)) {
                return;
            }
            long timeToWait = config.getLong("grace.graceTime");
            impendingPlayers.put(player,System.currentTimeMillis() + timeToWait);
            if (!config.getString("grace.titleText").isEmpty()) {
                player.networkHandler.sendPacket(new ClearTitleS2CPacket(true));
                player.networkHandler.sendPacket(new TitleFadeS2CPacket(0, (int) (timeToWait / 1000) * 20, 0));

                if (!config.getString("grace.titleText").isEmpty()) {
                    player.networkHandler.sendPacket(new TitleS2CPacket(Text.of(config.getString("grace.titleText"))));
                    if (!config.getString("grace.subtitleText").isEmpty()) {
                        player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.of(config.getString("grace.subtitleText"))));
                    }
                }
            }
            player.networkHandler.sendPacket(ServerPlayNetworking.createS2CPacket(InertiaAntiCheatConstants.REQUEST_PACKET_ID, PacketByteBufs.empty()));
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (Iterator<Map.Entry<ServerPlayerEntity, Long>> it = impendingPlayers.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<ServerPlayerEntity, Long> entry = it.next();
                if (entry.getValue() <= System.currentTimeMillis()) {
                    entry.getKey().networkHandler.sendPacket(new DisconnectS2CPacket(Text.of(config.getString("grace.disconnectMessage"))));
                    it.remove();
                }
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(InertiaAntiCheatConstants.RESPONSE_PACKET_ID, ModListResponseC2SPacket::receive);
    }
}
