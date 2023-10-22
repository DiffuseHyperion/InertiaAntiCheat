package me.diffusehyperion.inertiaanticheat.server;

import com.moandjiezana.toml.Toml;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants;
import me.diffusehyperion.inertiaanticheat.packets.ModListResponseC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.PingInfoC2SPacket;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheat.*;
import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants.CURRENT_SERVER_CONFIG_VERSION;

public class InertiaAntiCheatServer implements DedicatedServerModInitializer {

    public static HashMap<ServerPlayerEntity, Long> impendingPlayers = new HashMap<>();
    public static Toml serverConfig;
    public static KeyPair serverE2EEKeyPair; // null if e2ee not enabled

    @Override
    public void onInitializeServer() {
        info("Initializing InertiaAntiCheat!");
        serverConfig = initializeConfig("/config/server/InertiaAntiCheat.toml", CURRENT_SERVER_CONFIG_VERSION);
        debugInfo("Initializing listeners...");
        initializeListeners();
        debugInfo("Initializing E2EE...");
        serverE2EEKeyPair = initializeE2EE();
    }

    private void initializeListeners() {
        ServerPlayConnectionEvents.JOIN.register(this::onPlayerJoin);
        ServerTickEvents.END_SERVER_TICK.register(this::onEndServerTick);
        ServerPlayNetworking.registerGlobalReceiver(InertiaAntiCheatConstants.PING_PACKET_ID, PingInfoC2SPacket::receive);
        ServerPlayNetworking.registerGlobalReceiver(InertiaAntiCheatConstants.RESPONSE_PACKET_ID, ModListResponseC2SPacket::receive);
        debugInfo("Finished initializing listeners.");
    }

    private void onPlayerJoin(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender packetSender, MinecraftServer minecraftServer) {
        ServerPlayerEntity player = serverPlayNetworkHandler.player;

        if (Permissions.check(player, "inertiaanticheat.bypass")) {
            debugInfo("Player " + player.getEntityName() + " joined the server. Immediately allowing access as they have the bypass permission.");
            return;
        }

        long timeToWait = serverConfig.getLong("grace.graceTime");
        debugInfo("Player " + player.getName().getString() + " joined the server. Kicking them at: " + System.currentTimeMillis() + timeToWait);
        impendingPlayers.put(player, System.currentTimeMillis() + timeToWait);
        if (!serverConfig.getString("grace.titleText").isEmpty()) {
            player.networkHandler.sendPacket(new ClearTitleS2CPacket(true));
            player.networkHandler.sendPacket(new TitleFadeS2CPacket(0, (int) (timeToWait / 1000) * 20, 0));

            if (!serverConfig.getString("grace.titleText").isEmpty()) {
                player.networkHandler.sendPacket(new TitleS2CPacket(Text.of(serverConfig.getString("grace.titleText"))));
                debugInfo("Sending title packet, with the text: " + serverConfig.getString("grace.titleText"));
                if (!serverConfig.getString("grace.subtitleText").isEmpty()) {
                    player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.of(serverConfig.getString("grace.subtitleText"))));
                    debugInfo("Sending subtitle packet, with the text: " + serverConfig.getString("grace.subtitleText"));
                }
            }
        }

        if (Objects.nonNull(serverE2EEKeyPair)) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBytes(serverE2EEKeyPair.getPublic().getEncoded());
            debugInfo("Sending request with public key to player " + player.getName() + " with the length of " + serverE2EEKeyPair.getPublic().getEncoded().length);
            player.networkHandler.sendPacket(ServerPlayNetworking.createS2CPacket(InertiaAntiCheatConstants.REQUEST_PACKET_ID, buf));
        } else {
            debugInfo("Sending request to player " + player.getName().getString() + ".");
            player.networkHandler.sendPacket(ServerPlayNetworking.createS2CPacket(InertiaAntiCheatConstants.REQUEST_PACKET_ID, PacketByteBufs.empty()));
        }
    }

    private void onEndServerTick(MinecraftServer server) {
        for (Iterator<Map.Entry<ServerPlayerEntity, Long>> it = impendingPlayers.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<ServerPlayerEntity, Long> entry = it.next();
            if (entry.getValue() <= System.currentTimeMillis()) {
                entry.getKey().networkHandler.sendPacket(new DisconnectS2CPacket(Text.of(serverConfig.getString("grace.disconnectMessage"))));
                it.remove();
            }
        }
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
                warn("Private key file exists, but public key file does not! Backing up private key file...");
                File privateKeyFileBackup = getConfigDir().resolve("./" + privateKeyFileName + "-BACKUP.key").toFile();
                try {
                    privateKeyFileBackup.createNewFile();
                    Files.copy(privateKeyFile.toPath(), privateKeyFileBackup.toPath());
                } catch (IOException e) {
                    throw new RuntimeException("Something went wrong while backing up private key file!", e);
                }
            } else if (publicKeyFile.exists()) {
                warn("Public key file exists, but private key file does not! Backing up public key file now...");
                File publicKeyFileBackup = getConfigDir().resolve("./" + publicKeyFileName + "-BACKUP.key").toFile();
                try {
                    publicKeyFileBackup.createNewFile();
                    Files.copy(publicKeyFile.toPath(), publicKeyFileBackup.toPath());
                } catch (IOException e) {
                    throw new RuntimeException("Something went wrong while backing up public key file!", e);
                }
            }
            debugInfo("Generating new E2EE keypair now...");
            try {
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                keyPairGenerator.initialize(2048);
                KeyPair keyPair = keyPairGenerator.generateKeyPair();

                privateKey = keyPair.getPrivate();
                publicKey = keyPair.getPublic();

                privateKeyFile.createNewFile();
                publicKeyFile.createNewFile();
                Files.write(privateKeyFile.toPath(), privateKey.getEncoded());
                Files.write(publicKeyFile.toPath(), publicKey.getEncoded());

                debugInfo("Private key MD5 hash: " + InertiaAntiCheat.getHash(Arrays.toString(privateKey.getEncoded()), "MD5"));
                debugInfo("Public key MD5 hash: " + InertiaAntiCheat.getHash(Arrays.toString(publicKey.getEncoded()), "MD5"));
            } catch (NoSuchAlgorithmException | IOException e) {
                throw new RuntimeException("Something went wrong while generating new key pairs!", e);
            }
        } else {
            debugInfo("Found both key files.");
            try {
                Path privateKeyFilePath = Paths.get(privateKeyFile.toURI());
                byte[] privateKeyFileBytes = Files.readAllBytes(privateKeyFilePath);
                PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyFileBytes);

                Path publicKeyFilePath = Paths.get(publicKeyFile.toURI());
                byte[] publicKeyFileBytes = Files.readAllBytes(publicKeyFilePath);
                X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyFileBytes);

                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                privateKey = keyFactory.generatePrivate(privateKeySpec);
                publicKey = keyFactory.generatePublic(publicKeySpec);

                debugInfo("Private key MD5 hash: " + InertiaAntiCheat.getHash(Arrays.toString(privateKey.getEncoded()), "MD5"));
                debugInfo("Public key MD5 hash: " + InertiaAntiCheat.getHash(Arrays.toString(publicKey.getEncoded()), "MD5"));
            } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new RuntimeException("Something went wrong while reading key pairs!", e);
            }
        }
        return new KeyPair(publicKey, privateKey);
    }
}
