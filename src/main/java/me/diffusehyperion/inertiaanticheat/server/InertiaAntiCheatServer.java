package me.diffusehyperion.inertiaanticheat.server;

import com.moandjiezana.toml.Toml;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.packets.legacy.ModListResponseC2SPacket;
import me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import me.diffusehyperion.inertiaanticheat.util.Scheduler;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheat.*;
import static me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants.CURRENT_SERVER_CONFIG_VERSION;

public class InertiaAntiCheatServer implements DedicatedServerModInitializer {

    public static HashMap<ServerPlayerEntity, Long> impendingPlayers = new HashMap<>();
    public static Toml serverConfig;
    public static KeyPair serverE2EEKeyPair; // null if e2ee not enabled

    public static final Scheduler serverScheduler = new Scheduler();

    @Override
    public void onInitializeServer() {
        serverConfig = initializeConfig("/config/server/InertiaAntiCheat.toml", CURRENT_SERVER_CONFIG_VERSION);
        debugInfo("Initializing listeners...");
        initializeListeners();
        debugInfo("Initializing E2EE...");
        serverE2EEKeyPair = initializeE2EE();
    }

    private void initializeListeners() {
        ServerPlayConnectionEvents.JOIN.register(this::onPlayerJoin);
        ServerTickEvents.END_SERVER_TICK.register(this::onEndServerTick);
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
