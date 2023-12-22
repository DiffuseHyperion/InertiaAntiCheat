package me.diffusehyperion.inertiaanticheat.server;

import com.moandjiezana.toml.Toml;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.util.HashAlgorithm;
import me.diffusehyperion.inertiaanticheat.util.ModlistCheckMethod;
import me.diffusehyperion.inertiaanticheat.util.Scheduler;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.UUID;

import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheat.*;
import static me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants.CURRENT_SERVER_CONFIG_VERSION;

public class InertiaAntiCheatServer implements DedicatedServerModInitializer {

    public static HashMap<String, UUID> generatedKeys = new HashMap<>();
    public static Toml serverConfig;
    public static KeyPair serverE2EEKeyPair; // null if e2ee not enabled
    public static ModlistCheckMethod modlistCheckMethod;
    public static HashAlgorithm hashAlgorithm;

    public static final Scheduler serverScheduler = new Scheduler();

    //TODO: Add inertiaanticheat.bypass logic
    @Override
    public void onInitializeServer() {
        serverConfig = initializeConfig("/config/server/InertiaAntiCheat.toml", CURRENT_SERVER_CONFIG_VERSION);

        switch (serverConfig.getString("mods.method").toLowerCase()) {
            case "individual" -> modlistCheckMethod = ModlistCheckMethod.INDIVIDUAL;
            case "group" -> modlistCheckMethod = ModlistCheckMethod.GROUP;
            default -> {
                InertiaAntiCheat.error("There was an error in your config! Invalid method specified under \"mods.method\"! ");
                InertiaAntiCheat.error("Defaulting to individual method for now.");
                modlistCheckMethod = ModlistCheckMethod.INDIVIDUAL;
            }
        }

        switch (serverConfig.getString("mods.algorithm").toLowerCase()) {
            case "md5" -> hashAlgorithm = HashAlgorithm.MD5;
            case "sha1" -> hashAlgorithm = HashAlgorithm.SHA1;
            case "sha256" -> hashAlgorithm = HashAlgorithm.SHA256;
            default -> {
                InertiaAntiCheat.error("There was an error in your config! Invalid algorithm specified under \"mods.algorithm\"! ");
                InertiaAntiCheat.error("Defaulting to MD5 algorithm for now.");
                hashAlgorithm = HashAlgorithm.MD5;
            }
        }

        debugInfo("Initializing E2EE...");
        serverE2EEKeyPair = initializeE2EE();

        ServerPlayConnectionEvents.JOIN.register(this::onPlayerJoin);
    }

    private void onPlayerJoin(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender packetSender, MinecraftServer minecraftServer) {
        InertiaAntiCheat.info("Player joining from address: " + serverPlayNetworkHandler.getConnectionAddress());
    }

    private KeyPair initializeE2EE() {
        if (!serverConfig.getBoolean("e2ee.enable")) {
            debugInfo("E2EE was not enabled. Skipping e2ee initialization...");
            return null;
        }

        String privateKeyFileName = serverConfig.getString("e2ee.privateKeyName");
        String publicKeyFileName = serverConfig.getString("e2ee.publicKeyName");
        File privateKeyFile = getConfigDir().resolve("./" + privateKeyFileName).toFile();
        File publicKeyFile = getConfigDir().resolve("./" + publicKeyFileName).toFile();
        PrivateKey privateKey;
        PublicKey publicKey;

        if (!privateKeyFile.exists() && !publicKeyFile.exists()) {
            warn("E2EE was enabled, but the mod did not find either the private or public key file! Generating new keypair now...");
            warn("This is fine if this is the first time you are running the mod.");
            if (privateKeyFile.exists()) {
                warn("Private key file exists, but public key file does not! Backing up and deleting private key file...");
                try {
                    File privateKeyFileBackup = getConfigDir().resolve("./" + privateKeyFileName + "-BACKUP.key").toFile();
                    privateKeyFileBackup.createNewFile();
                    Files.copy(privateKeyFile.toPath(), privateKeyFileBackup.toPath());
                    privateKeyFile.delete();
                } catch (IOException e) {
                    throw new RuntimeException("Something went wrong while backing up private key file! The mod has not deleted your private key file. Please delete " + privateKeyFileName + " by yourself.", e);
                }
            } else if (publicKeyFile.exists()) {
                warn("Public key file exists, but private key file does not! Backing up public key file now...");
                try {
                    File publicKeyFileBackup = getConfigDir().resolve("./" + publicKeyFileName + "-BACKUP.key").toFile();
                    publicKeyFileBackup.createNewFile();
                    Files.copy(publicKeyFile.toPath(), publicKeyFileBackup.toPath());
                    publicKeyFile.delete();
                } catch (IOException e) {
                    throw new RuntimeException("Something went wrong while backing up public key file! The mod has not deleted your public key file. Please delete " + publicKeyFileName + " by yourself.", e);
                }
            }
            debugInfo("Generating new E2EE keypair now...");
            KeyPair newPair = InertiaAntiCheat.createRSAPair(publicKeyFile, privateKeyFile);
            publicKey = newPair.getPublic();
            privateKey = newPair.getPrivate();
        } else {
            debugInfo("Found both key files.");
            KeyPair newPair = InertiaAntiCheat.loadRSAPair(publicKeyFile, privateKeyFile);
            publicKey = newPair.getPublic();
            privateKey = newPair.getPrivate();
        }
        return new KeyPair(publicKey, privateKey);
    }
}
