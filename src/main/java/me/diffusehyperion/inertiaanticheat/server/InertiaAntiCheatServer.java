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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
        InertiaAntiCheat.debugInfo("Initializing InertiaAntiCheat!");
        InertiaAntiCheat.debugInfo("Initializing end-to-end encryption...");
        initializeE2EE();
        InertiaAntiCheat.debugInfo("Initializing config...");
        initializeConfig();
        InertiaAntiCheat.debugInfo("Initializing listeners...");
        initializeListeners();
    }

    private void initializeConfig() {
        File configFile = getConfigDir().resolve("InertiaAntiCheat.toml").toFile();
        if (!configFile.exists()) {
            LOGGER.warn("No config file found! Creating a new one now...");
            try {
                Files.copy(Objects.requireNonNull(InertiaAntiCheatServer.class.getResourceAsStream("/InertiaAntiCheat.toml")), configFile.toPath());
            } catch (IOException e) {
                throw new RuntimeException("Couldn't a create default config!", e);
            }
        }
        config = new Toml().read(configFile);
        InertiaAntiCheat.debugInfo("Config version: " + config.getLong("debug.version"));
        if (config.getLong("debug.version", 0L) != CURRENT_CONFIG_VERSION) {
            LOGGER.warn("Looks like your config file is outdated! Backing up current config, then creating an updated config.");
            LOGGER.warn("Your config file will be backed up to \"BACKUP-InertiaAntiCheat.toml\".");
            File backupFile = getConfigDir().resolve("BACKUP-InertiaAntiCheat.toml").toFile();
            try {
                Files.copy(configFile.toPath(), backupFile.toPath());
            } catch (IOException e) {
                throw new RuntimeException("Couldn't copy existing config file into a backup config file! Please do it manually.", e);
            }
            if (!configFile.delete()) {
                throw new RuntimeException("Couldn't delete config file! Please delete it manually.");
            }
            try {
                Files.copy(Objects.requireNonNull(InertiaAntiCheatServer.class.getResourceAsStream("/InertiaAntiCheat.toml")), configFile.toPath());
            } catch (IOException e) {
                throw new RuntimeException("Couldn't create a default config!", e);
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

    private void initializeE2EE() {
        if (!config.getBoolean("e2ee.enabled")) {
            return;
        }
        String privateKeyName = config.getString("e2ee.privateKey");
        String publicKeyName = config.getString("e2ee.publicKey");
        File privateKey = getConfigDir().resolve(config.getString("e2ee.privateKey")).toFile();
        File publicKey = getConfigDir().resolve(config.getString("e2ee.publicKey")).toFile();
        if (!privateKey.exists() || !publicKey.exists()) {
            if (!privateKey.exists()) {
                LOGGER.error("Could not find your private key! The mod will generate a new key pair for you.");
                if (!publicKey.exists() && privateKey.exists()) {
                    String backupFileName = "BACKUP-" + config.getString("e2ee.publicKey");
                    File backupFile = getConfigDir().resolve(backupFileName).toFile();
                    LOGGER.error("It looks like your public key still exists. The mod will rename the file to " + backupFileName + ".");
                    try {
                        Files.copy(publicKey.toPath(), backupFile.toPath());
                    } catch (IOException e) {
                        throw new RuntimeException("Couldn't copy existing public key into a backup public key file!", e);
                    }
                    if (!publicKey.delete()) {
                        throw new RuntimeException("Could not delete the old public key file! Please delete it manually.");
                    }
                }
            } else if (!publicKey.exists()) {
                LOGGER.error("Could not find your public key! The mod will generate a new key pair for you.");
                if (!privateKey.exists() && publicKey.exists()) {
                    String backupFileName = "BACKUP-" + config.getString("e2ee.privateKey");
                    File backupFile = getConfigDir().resolve(backupFileName).toFile();
                    LOGGER.error("It looks like your private key still exists. The mod will rename the file to " + backupFileName + ".");
                    try {
                        Files.copy(publicKey.toPath(), backupFile.toPath());
                    } catch (IOException e) {
                        throw new RuntimeException("Couldn't copy existing private key into a backup public key file!", e);
                    }
                    if (!publicKey.delete()) {
                        throw new RuntimeException("Could not delete the old private key file! Please delete it manually.");
                    }
                }
            } else if (!privateKey.exists() && !publicKey.exists()) {
                LOGGER.warn("Could not find either private and public keys! This is fine if this is your first run.");
            }

            InertiaAntiCheat.debugInfo("Generating new key pair...");
            KeyPairGenerator generator;
            try {
                generator = KeyPairGenerator.getInstance("RSA");
            } catch (NoSuchAlgorithmException e) {
                // should never happen
                throw new RuntimeException(e);
            }
            SecureRandom random = new SecureRandom();
            generator.initialize(2048, random);
            KeyPair pair = generator.generateKeyPair();

            InertiaAntiCheat.debugInfo("Writing new key pair to files...");
            try (FileOutputStream fos = new FileOutputStream(privateKey)) {
                fos.write(pair.getPrivate().getEncoded());
            } catch (IOException e) {
                throw new RuntimeException("Couldn't write your private key into " + privateKeyName + "!", e);
            }
            try (FileOutputStream fos = new FileOutputStream(publicKey)) {
                fos.write(pair.getPublic().getEncoded());
            } catch (IOException e) {
                throw new RuntimeException("Couldn't write your public key into " + publicKeyName + "!", e);
            }
            LOGGER.warn("Finished creating new key pair! Do not share the private key file with anybody!");
        }
    }

    private Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
